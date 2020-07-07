package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class GameContract : Contract {
    companion object { // If you need a function or a property to be tied to a class rather than to instances of it (like static in java), you can declare it inside a companion object:
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.GameContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // transaction must have a single command from the commands defined in this contract
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> requireThat {
                "No input states should be consumed when creating a game" using (tx.inputs.isEmpty())
                "There should be just one output state in game creation" using (tx.outputs.size==1)
                val board = tx.outputsOfType<BoardState>().single() // additional check for single BoardState output
                "The players on the board must be different identities" using (board.playerNought != board.playerCross)
                "Create transaction signers must be both the players on the board and only them" using (command.signers.toSet() == board.participants.map { it.owningKey }.toSet())
            }
            is Commands.Play -> requireThat {

            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands // do we need TypeOnlyCommandDta? TypeOnlyCommandData is a helpful utility for the case when there’s no data inside the command; only the existence matters.
        class Play : Commands // place a symbol. needs arguments
    }
}