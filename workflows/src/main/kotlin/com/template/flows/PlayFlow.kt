package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * This is the flow which handles the (partial) settlement of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class PlayFlow(// val linearId: UniqueIdentifier,
        val opponent: Party,
//        val pos: Pair<Int,Int>
//        val x: Int,
//        val y: Int
val pos: Int
): FlowLogic<SignedTransaction>() {

    companion object {
        object QUERYING_VAULT: ProgressTracker.Step("Querying vault for active game")
        object GENERATING_TX : ProgressTracker.Step("PlayFlow: Generating transaction")
        object CHECKING_OUTPUT: ProgressTracker.Step("Checking outcome and creating the respective output state")
        object VERIFYING_TX: ProgressTracker.Step("Verifying against contract constraints")
        object SIGNING_TX: ProgressTracker.Step("Signing tx myself")
        object GATHERING_SIGS: ProgressTracker.Step("Gathering counterparty's signature") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }
        object FINALISING_TX: ProgressTracker.Step("Getting notary signature and recording the tx") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
        object GAME_OVER: ProgressTracker.Step("Starting GameOverFlow")
        fun tracker() = ProgressTracker(QUERYING_VAULT,GENERATING_TX,CHECKING_OUTPUT,VERIFYING_TX,SIGNING_TX,GATHERING_SIGS,FINALISING_TX,GAME_OVER)
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = QUERYING_VAULT
        // Step 1. Retrieve the state from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(exactParticipants = listOf(ourIdentity, opponent))
        val inputStateAndRef = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        val input = inputStateAndRef.state.data
        // flow initiation rules
        if (!input.participants.contains(ourIdentity)) {
            throw IllegalArgumentException("Only one of the state's participants can play")
        }
        if (ourIdentity == input.whoseTurn) {
            throw IllegalArgumentException("The two players must alternate turns i.e. the same player cannot play twice in a row") // TODO dont print entire stack trace in log, message to user
        }
        // Step 3. Create a transaction builder.
        progressTracker.currentStep = GENERATING_TX
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        val command = Command(GameContract.Commands.Play(), listOf(opponent.owningKey, ourIdentity.owningKey))
        builder.addCommand(command)
        builder.addInputState(inputStateAndRef)
        // check if output state is a game-over
        progressTracker.currentStep = CHECKING_OUTPUT
        val output = input.writeSymbol(ourIdentity, pos)
        val outcome = BoardState.checkOutcome(output.board)
        if (outcome == BoardState.Outcome.IN_PROGRESS) { // game not over
            builder.addOutputState(output, GameContract.ID)
        }
        else {
            builder.addOutputState(output.updateOutcome(), GameContract.ID)
        }
        // Step 8. Verify and sign the transaction.
        progressTracker.currentStep = VERIFYING_TX
        builder.verify(serviceHub)
        progressTracker.currentStep = SIGNING_TX
        val ptx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        progressTracker.currentStep = GATHERING_SIGS
        val session = initiateFlow(opponent)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session))) // idk why CollectSignatureFlow (singular) doesnt return stx
        progressTracker.currentStep = FINALISING_TX
        val finalStx = subFlow(FinalityFlow(stx, session))
        // if game over, start subflow that ends the game
        return if (outcome == BoardState.Outcome.IN_PROGRESS) finalStx
        else {
            progressTracker.currentStep = GAME_OVER
            subFlow(GameOverFlow(opponent))
            finalStx
        }
    }
}

/**
 * This is the flow which signs IOU settlements.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(PlayFlow::class)
class PlayResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // signing transaction
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                //TODO
//                val contract = GameContract()
//                contract.verify(stx.toLedgerTransaction(serviceHub))
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}




