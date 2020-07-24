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
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

@InitiatingFlow
class GameOverFlow(val opponent: Party
) : FlowLogic<SignedTransaction>() {

    companion object {
        object QUERYING_VAULT: Step("Querying vault for game that should be in the gameover state")
        object GENERATING_TX : Step("GameOverFlow: Generating transaction with 1 input state and 0 output state")
        object VERIFYING_TX: Step("Verifying against contract constraints")
        object SIGNING_TX: Step("Signing tx myself")
        object GATHERING_SIGS: Step("Gathering counterparty's signature") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }
        object FINALISING_TX: Step("Getting notary signature and recording the tx") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
        fun tracker() = ProgressTracker(QUERYING_VAULT,GENERATING_TX,VERIFYING_TX,SIGNING_TX,GATHERING_SIGS,FINALISING_TX)
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = QUERYING_VAULT
        // Step 1. Retrieve the state from the vault.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(exactParticipants = listOf(ourIdentity, opponent))
        val inputStateAndRef = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        val input = inputStateAndRef.state.data
        // build tx
        progressTracker.currentStep = GENERATING_TX
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        val command = Command(GameContract.Commands.EndGame(), input.participants.map { it.owningKey })
        builder.addCommand(command)
        builder.addInputState(inputStateAndRef)
        // verify with the contract
        progressTracker.currentStep = VERIFYING_TX
        builder.verify(serviceHub)
        // sign
        progressTracker.currentStep = SIGNING_TX
        val ptx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        progressTracker.currentStep = GATHERING_SIGS
        val session = initiateFlow(opponent)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session))) // session not necessary
//        val stx = serviceHub.addSignature(ptx, input.playerO.owningKey) // i'm guessing can't do this cuz playerO is not us. but who is us??
        // finalize with notary
        progressTracker.currentStep = FINALISING_TX
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

