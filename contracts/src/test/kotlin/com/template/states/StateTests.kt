package com.template.states

import net.corda.core.contracts.LinearState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for BoardState class
 */
class StateTests {
    private val alice = TestIdentity(CordaX500Name("Alice","London","GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob","London","GB")).party

    @Test
    fun `BoardState implements LinearState`() {
        assert(LinearState::class.java.isAssignableFrom(BoardState::class.java))
    }

    @Test
    fun `BoardState has player fields of correct type`() {
        // Do the fields playerX and playerO exist?
        BoardState::class.java.getDeclaredField("playerX");
        BoardState::class.java.getDeclaredField("playerO");
        // Are those fields of the correct type?
        assertEquals(BoardState::class.java.getDeclaredField("playerX").type, Party::class.java);
        assertEquals(BoardState::class.java.getDeclaredField("playerO").type, Party::class.java);
    }

    @Test
    fun `BoardState has winner and board field of correct type`() {
        // does the field 'winner' and 'board' exist
        BoardState::class.java.getDeclaredField("winner");
        BoardState::class.java.getDeclaredField("board");
        // is the field of correct type
        assertEquals(BoardState::class.java.getDeclaredField("winner").type, BoardState.Symbol::class.java);
        assertEquals(BoardState::class.java.getDeclaredField("board").type, Array<Array<BoardState.Symbol>>::class.java);
    }

    @Test
    fun `playerX and playerO are participants`() {
        val boardState = BoardState(alice,bob)
        assertNotEquals(-1, boardState.participants.indexOf(alice))
        assertNotEquals(-1, boardState.participants.indexOf(bob))
    }

    @Test
    fun `BoardState fields are ordered correctly`() {
        val fields = BoardState::class.java.declaredFields
        val playerXIdx = fields.indexOf(BoardState::class.java.getDeclaredField("playerX"))
        val playerOIdx = fields.indexOf(BoardState::class.java.getDeclaredField("playerO"))
        val boardIdx = fields.indexOf(BoardState::class.java.getDeclaredField("board"))
        val winnerIdx = fields.indexOf(BoardState::class.java.getDeclaredField("winner"))
        val linearIdIdx = fields.indexOf(BoardState::class.java.getDeclaredField("linearId"))

        assert(playerXIdx < playerOIdx)
        assert(playerOIdx < boardIdx)
        assert(boardIdx < winnerIdx)
        assert(winnerIdx < linearIdIdx)
    }

    @Test
    fun `writeSymbol() writes the correct symbol on the correct position`() {
        val boardState = BoardState(alice,bob)
        assertFalse {BoardState.Symbol.O in boardState.board.flatten()  }
        assertFalse {BoardState.Symbol.X in boardState.board.flatten()  }
        // insert a symbol into an empty board
        val newBoard = boardState.writeSymbol(Pair(1,2), BoardState.Symbol.X)
        assertTrue { BoardState.Symbol.X in newBoard.board.flatten() }
        assertTrue { newBoard.board[1][2] == BoardState.Symbol.X }
    }

    @Test
    fun `writeSymbol() only replaces one symbol without affecting the rest of the board`() {
        val boardOne = BoardState(alice,bob).writeSymbol(Pair(1,2), BoardState.Symbol.X).writeSymbol(Pair(1,0), BoardState.Symbol.O)
        assertTrue { boardOne.board[1][2] == BoardState.Symbol.X }
        assertTrue { boardOne.board[1][0] == BoardState.Symbol.O }
        val boardTwo = boardOne.writeSymbol(Pair(1,2), BoardState.Symbol.O)
        assertTrue { boardTwo.board[1][2] == BoardState.Symbol.O }
        assertTrue { boardTwo.board[1][0] == BoardState.Symbol.O }
    }
}