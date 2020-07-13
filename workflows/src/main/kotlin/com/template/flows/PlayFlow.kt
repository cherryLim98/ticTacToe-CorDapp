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
        val x: Int,
        val y: Int
): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Step 1. Retrieve the state from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(exactParticipants = listOf(ourIdentity, opponent))
        val inputStateAndRef = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        val input = inputStateAndRef.state.data
        // flow initiation rules
        if (!input.participants.contains(ourIdentity)) {
            throw IllegalArgumentException("Only one of the state's participants can play")
        }
        if (ourIdentity == input.whoseTurn) {
            throw IllegalArgumentException("The two players must alternate turns i.e. the same player cannot play twice in a row")
        }
        // Step 3. Create a transaction builder.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        val command = Command(GameContract.Commands.Play(), listOf(opponent.owningKey, ourIdentity.owningKey))
        builder.addCommand(command)
        builder.addInputState(inputStateAndRef)
        // check if output state is a game-over
        val pos = Pair(x,y)
        val output = input.writeSymbol(ourIdentity, pos)
        val outcome = BoardState.checkOutcome(output.board)
        if (outcome == BoardState.Outcome.IN_PROGRESS) { // game not over
            builder.addOutputState(output, GameContract.ID)
        }
        else {
            builder.addOutputState(output.updateOutcome(), GameContract.ID)
        }
        // Step 8. Verify and sign the transaction.
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        val session = initiateFlow(opponent)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session))) // idk why CollectSignatureFlow (singular) doesnt return stx
        val finalStx = subFlow(FinalityFlow(stx, session))
        // if game over, start subflow that ends the game
        return if (outcome == BoardState.Outcome.IN_PROGRESS) finalStx
        else {
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
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}




