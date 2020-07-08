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
 *   array index = x + 3y
 */

@BelongsToContract(GameContract::class)
data class BoardState(//data class primary constructor must have properties declared as val
        val playerX: Party,
        val playerO: Party,
        val board: Array<Symbol> = Array(9) {  Symbol.U },
                      //val board: Array<Array<Symbol>> = Array(3) { Array(3) { Symbol.U} },
        val whoseTurn: Party? = null,
                      /*val posPlayedThisTurn: Pair<Int,Int>, // pair(x,y)
                      val symbolPlayedThisTurn: Symbol = Symbol.X, */
        val winner: Symbol = Symbol.U,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    @CordaSerializable
    enum class Symbol {
        O, X, U // U for undefined
    }

    companion object {
        fun getSymbolAdded(oldBoard: Array<Symbol>, newBoard: Array<Symbol>) : Symbol {
            for (i in 0..8) {
                if (oldBoard[i] == Symbol.U && newBoard[i] != oldBoard[i]) return newBoard[i]
            }
            return Symbol.U
        }
        fun getChangedPos(oldBoard: Array<Symbol>, newBoard: Array<Symbol>) : Pair<Int,Int> {
            for (i in 0..8) {
                if (oldBoard[i] == Symbol.U && newBoard[i] != oldBoard[i]) {
                    val x = i % 3
                    val y = (i - x) / 3
                    return Pair(x,y)
                }
            }
            return Pair(-1,-1)
        }
    }

    // ============ FIELDS ================
    override val participants: List<AbstractParty> = listOf(playerX,playerO)

    //=============== METHODS ==================
    /**
    fun writeSymbol(pos: Pair<Int,Int>, symbol: Symbol): BoardState {
        val newBoard = Array(3) {i ->
            if (i == pos.first) Array(3) {j ->
                if (j == pos.second) symbol
                else board[i][j]
            } else board[i]
        }
        return copy(board = newBoard)
    }
    // arrayOf of mutableListOf
    */

    fun writeSymbol(player:Party, pos: Pair<Int,Int> /*, symbol: Symbol*/): BoardState {
        val symbol : Symbol = if (player==playerX) {
            Symbol.X
        }
        else {
            Symbol.O
        }
        val newBoard = Array(9) { idx->
            if (idx == pos.first + 3 * pos.second) symbol
            else board[idx]
        }
        return copy(board = newBoard, whoseTurn = player)
    }



    fun getFromBoard(pos:Pair<Int,Int>) : Symbol {
        return board[pos.first + 3 * pos.second]
    }

    fun replaceBoard(newBoard: Array<Symbol>) = copy(board = newBoard)

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

