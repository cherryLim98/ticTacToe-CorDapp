package com.template.states

import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/**
 * A Noughts and Crosses 3x3 board.
 * notes
 * - data: public final. final cuz we dont want surprises
 *
 * ====BOARD===
 * y
 * 2 _   _   _
 * 1 _   _   _
 * 0 _   _   _
 *   0   1   2  x
 */

@BelongsToContract(TemplateContract::class)
data class BoardState(//data class primary constructor must have properties declared as val
                      val playerNought: Party,
                      val playerCross: Party,
                      val board: Array<Array<Symbol>> = Array(3) { Array(3) { Symbol.U} },
                      val winner: Symbol = Symbol.U,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    enum class Symbol {
        O, X, U // U for undefined
    }

    // ============ FIELDS ================
    override val participants: List<AbstractParty> = listOf(playerNought,playerCross)

    //=============== METHODS ==================
    fun writeSymbol(pos: Pair<Int,Int>, symbol: Symbol): BoardState {
        val newBoard = Array(3) {idx ->
            if (idx == pos.first) Array(3) {idx ->
                if (idx == pos.second) symbol
                else Symbol.U
            } else Array(3) {Symbol.U}
        }
        return copy(board = newBoard)

    }

    //============== intellij asked me to do this =====================
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoardState

        if (playerNought != other.playerNought) return false
        if (playerCross != other.playerCross) return false
        if (!board.contentDeepEquals(other.board)) return false
        if (participants != other.participants) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playerNought.hashCode()
        result = 31 * result + playerCross.hashCode()
        result = 31 * result + board.contentDeepHashCode()
        result = 31 * result + participants.hashCode()
        return result
    }
}
