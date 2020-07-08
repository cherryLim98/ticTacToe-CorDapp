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
                val state = tx.outputsOfType<BoardState>().single() // additional check for single BoardState output
                "The players on the board must be different identities" using (state.playerX != state.playerO)
                "Create transaction signers must be both the players on the board and only them" using (command.signers.toSet() == state.participants.map { it.owningKey }.toSet())
                "There must be no winner" using (state.winner == BoardState.Symbol.U)
            }
            is Commands.Play -> requireThat {
                "There must be one input state" using (tx.inputs.size==1)
                "There must be one output state" using (tx.outputs.size==1)
                val input = tx.inputsOfType<BoardState>().single()
                val output = tx.outputsOfType<BoardState>().single()
                "Players and linearID must not change" using (input.playerX == output.playerX && input.playerO == output.playerO && input.linearId == output.linearId)
                "You must place a symbol on a blank slot" using (output.board.count { it == BoardState.Symbol.U } < input.board.count { it == BoardState.Symbol.U })
                val symbolThisTurn = BoardState.getSymbolAdded(input.board,output.board)
                "Player must play only the symbol they were assigned to" using (
                        if (output.whoseTurn == output.playerX) symbolThisTurn == BoardState.Symbol.X
                        else symbolThisTurn == BoardState.Symbol.O
                        )
                "playerX and playerO must alternate turns" using (input.whoseTurn != output.whoseTurn)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands // do we need TypeOnlyCommandDta? TypeOnlyCommandData is a helpful utility for the case when there’s no data inside the command; only the existence matters.
        class Play : Commands // place a symbol. needs arguments
    }
}