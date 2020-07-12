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
class CreateFlow(val opponent: Party
)
    : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val state = BoardState(ourIdentity, opponent)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(exactParticipants = listOf(ourIdentity, opponent))
        val boardStatesWithUsTwo = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states
        if (boardStatesWithUsTwo.isNotEmpty()) {
            throw IllegalArgumentException("You can only have one ongoing game with your chosen player at a time")
        }
        // build transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GameContract.Commands.Create(), state.participants.map { it.owningKey })
        val builder = TransactionBuilder(notary)
        builder.addOutputState(state, GameContract.ID) // addOutputState() requires a notary idk why
        builder.addCommand(command)
        // Step 5. Verify with contract
        builder.verify(serviceHub)
        // all the signing
        val ptx = serviceHub.signInitialTransaction(builder)
        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        // check with notary
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(CreateFlow::class)
class CreateResponder(val session: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an BoardState transaction" using (output is BoardState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = session, expectedTxId = txWeJustSignedId.id))
    }
}