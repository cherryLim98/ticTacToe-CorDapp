package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class PlayFlow(val linearId: UniqueIdentifier,
               val pos: Pair<Int,Int>
               )
    : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiator flow logic goes here.
        // Stage 1. Retrieve state specified by linearId from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val boardStateAndRef =  serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        val input = boardStateAndRef.state.data
        // this flow must be initiated by one of the players
        if (!input.participants.contains(ourIdentity)) {
            throw IllegalArgumentException("Only one of the predefined players can play")
        }
        // this flow can only be initiated if the previous state was played on by the other player OR this is the first turn ever
        if (ourIdentity == input.whoseTurn) {
            throw IllegalArgumentException("The two players must alternate turns i.e. the same player cannot play twice in a row")
        }
        // create new BoardState with this player's symbol written on the board
        val output = input.writeSymbol(ourIdentity, pos)
        // create the play command
        val signers = input.participants.map { it.owningKey }
        val playCommand = Command(GameContract.Commands.Play(), signers)
        // Stage 5. Get a reference to a transaction builder.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary = notary)
        // Stage 6. Create the transaction which comprises one input, one output and one command.
        builder.withItems(boardStateAndRef,
                StateAndContract(output, GameContract.ID),
                playCommand)
        // Stage 7. Verify and sign the transaction.
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Stage 8. Collect signature from the other player and add it to the transaction.
        // This also verifies the transaction and checks the signatures.
        val sessions = (input.participants - ourIdentity ).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        // Stage 9. Notarise and record the transaction in our vaults.
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(PlayFlow::class)
class PlayResponder(val session: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.
        val signedTransactionFlow = object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a BoardState transaction" using (output is BoardState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = session, expectedTxId = txWeJustSignedId.id))

    }
}
