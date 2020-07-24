package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class GameOverFlow(val opponent: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Step 1. Retrieve the state from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(exactParticipants = listOf(ourIdentity, opponent))
        val inputStateAndRef = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        val input = inputStateAndRef.state.data
        // build tx
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        val command = Command(GameContract.Commands.EndGame(), input.participants.map { it.owningKey })
        builder.addCommand(command)
        builder.addInputState(inputStateAndRef)
        // verify with the contract
        builder.verify(serviceHub)
        // sign
        val ptx = serviceHub.signInitialTransaction(builder, input.playerX.owningKey)
        val session = initiateFlow(input.playerO)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session))) // session not necessary
//        val stx = serviceHub.addSignature(ptx, input.playerO.owningKey) // i'm guessing can't do this cuz playerO is not us. but who is us??
        // finalize with notary
        return subFlow(FinalityFlow(stx, session))
    }


    @InitiatedBy(GameOverFlow::class)
    class GameOverResponder(val session: FlowSession): FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            // sign transaction
            val stxFlow = object: SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) {
                    //
                }
            }
            val txWeSigned = subFlow(stxFlow)

            return subFlow(ReceiveFinalityFlow(otherSideSession = session , expectedTxId = txWeSigned.id ))
        }
    }
}

