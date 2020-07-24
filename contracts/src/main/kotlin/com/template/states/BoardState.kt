package com.template.states

import com.template.contracts.GameContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

/**
 * A Noughts and Crosses 3x3 board.
 * notes
 * - data: public final. final cuz we dont want surprises
 *
 * ====BOARD===
 * y
 * 2  _6   _7   _8
 * 1  _3   _4   _5
 * 0  _0   _1   _2
 *    0    1    2  x
 *
 *    idx = x + 3y
 *    x = idx % 3; y = (idx - x) / 3
 */

@BelongsToContract(GameContract::class)
data class BoardState(//data class primary constructor must have properties declared as val
        val playerX: Party,
        val playerO: Party,
        val board: Array<Symbol> = Array(9) {  Symbol.U },
        val whoseTurn: Party? = null,
        val outcome: String = "game still in progress", // TODO find a better data type for this
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    // ============ FIELDS ================
    override val participants: List<AbstractParty> = listOf(playerX,playerO)

    @CordaSerializable
    enum class Symbol {
        O, X, U // U for blank
    }
    @CordaSerializable
    enum class Outcome {
        X_WINS, O_WINS, DRAW, IN_PROGRESS
    }

    companion object {
        // ========================= STATIC METHODS ===============================
        fun getSymbolAdded(oldBoard: Array<Symbol>, newBoard: Array<Symbol>) : Symbol {
            for (i in 0..8) {
                if (oldBoard[i] == Symbol.U && newBoard[i] != oldBoard[i]) return newBoard[i]
            }
            return Symbol.U
        }
        fun checkOutcome(board: Array<Symbol>) : Outcome {
            // find a group of positions where all marks are the same
            for (triple in winningTriples()) {
                var threeMatchingNonBlankSymbols = true // remains true if the symbols at all positions in this group on the grid are the same
                val first: Symbol = board[triple.first] // record char at first position in the group
                for (winIdx in triple.toList()) {
                    val symbol: Symbol = board[winIdx]
                    // ensure mark at every position in this group matches the first
                    if (symbol == Symbol.U || symbol != first) {
                        threeMatchingNonBlankSymbols = false
                        break
                    }
                }
                if (threeMatchingNonBlankSymbols) { // if the marks at all positions in this group match, it means this symbol has won
                    return if (first == Symbol.X) Outcome.X_WINS
                    else Outcome.O_WINS
                }
            }
            // if no winning symbol is found
            return if (board.count { it==Symbol.U } == 0) Outcome.DRAW
            else Outcome.IN_PROGRESS
        }

        private fun winningTriples() :  Array<Triple<Int,Int,Int>> {
            var array = Array<Triple<Int,Int,Int>>(8) {Triple(-1,-1,-1)}
            array[0] = Triple(0,1,2)
            array[1] = Triple(3,4,5)
            array[2] = Triple(6,7,8)
            array[3] = Triple(0,3,6)
            array[4] = Triple(1,4,7)
            array[5] = Triple(2,5,8)
            array[6] = Triple(0,4,8)
            array[7] = Triple(2,4,6)
            return array
        }
    }


    //=============== METHODS ==================
    fun writeSymbol(player:Party, idx:Int /*pos: Pair<Int,Int>*/): BoardState {
        val symbol : Symbol = if (player==playerX) Symbol.X else Symbol.O
        val newBoard = Array(9) { i->
//            if (idx == pos.first + 3 * pos.second && board[idx] == Symbol.U) symbol // if player maliciously tries to replace a non-blank symbol then the same board is returned
            if (i==idx && board[i]==Symbol.U) symbol
            else board[i]
        }
        return copy(board = newBoard, whoseTurn = player)
    }
    // only called if a Win or Draw occurs
    fun updateOutcome(): BoardState {
        val outcome = checkOutcome(board)
        return copy(outcome = if (outcome == Outcome.X_WINS) "${playerX.name} wins" else if (outcome == Outcome.O_WINS) "${playerO.name} wins" else "Draw")
    }



        //============== intellij asked me to do this =====================
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BoardState

            if (playerX != other.playerO) return false
            if (playerX != other.playerO) return false
            if (!board.contentDeepEquals(other.board)) return false
            if (participants != other.participants) return false

            return true
        }

        override fun hashCode(): Int {
            var result = playerX.hashCode()
            result = 31 * result + playerO.hashCode()
            result = 31 * result + board.contentDeepHashCode()
            result = 31 * result + participants.hashCode()
            return result
        }
}


