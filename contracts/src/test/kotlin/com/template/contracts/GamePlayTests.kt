//
//package com.template.contracts
//
//import com.template.states.BoardState
//import net.corda.core.contracts.TypeOnlyCommandData
//import net.corda.core.identity.CordaX500Name
//import net.corda.testing.contracts.DummyState
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import org.junit.Test
//
//
///**
// * Tests for Play Command
// */
//
//class GamePlayTests {
//    class DummyCommand : TypeOnlyCommandData()
//
//    private val ledgerServices = MockServices()
//    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB"))
//    private val bob = TestIdentity(CordaX500Name("Bob", "London", "GB"))
//    private val carl = TestIdentity(CordaX500Name("Carl", "New York", "US"))
//
//    /**
//    @Test
//    fun `There must be only one input state and one output state`() {
//        val board = BoardState(alice.party, bob.party, posPlayedThisTurn = Pair(0,1))
//        ledgerServices.ledger {
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Play())
//                this `fails with` "There must be one input state"
//            }
//            transaction {
//                input(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Play())
//                this `fails with` "There must be one output state"
//            }
//
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Play())
//                this.verifies()
//            }
//        }
//    }*/
//
//    /**
//    @Test
//    fun `Only the board property in BoardState may change`() {
//        val board = BoardState(alice.party,bob.party)
//        ledgerServices.ledger {
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, BoardState(alice.party, carl.party))
//                command(listOf(alice.publicKey,carl.publicKey), GameContract.Commands.Play())
//                this `fails with` "Only the board property in BoardState may change"
//            }
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board.writeSymbol(Pair(0,1),BoardState.Symbol.X))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this.verifies()
//            }
//        }
//    }*/
//
//    // ====================== GAME LOGIC ======================
//    /**
//     * Players cannot put a blank symbol (so they 1. cannot erase a nonblank (X/O) symbol. 2.  must put a nonblank (X/O) symbol down)
//     */
//    @Test
//    fun `The U Symbol cannot be played`() {
//        val board = BoardState(alice.party,bob.party, posPlayedThisTurn = Pair(0,1))
//        ledgerServices.ledger {
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, BoardState(alice.party,bob.party, board = board.board, posPlayedThisTurn = Pair(0,1), symbolPlayedThisTurn = BoardState.Symbol.U))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this `fails with` "The U Symbol cannot be played"
//            }
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board.writeSymbol(Pair(0,1),BoardState.Symbol.X))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this.verifies()
//            }
//        }
//
//    }
//
//
///**
//     * Players also cannot replace any existing nonblank symbol on the board. They have to put their symbol on a blank (U) space.
//     */
//
//    @Test
//    fun `The number of blanks must decrease`() {
//        val board = BoardState(alice.party,bob.party).writeSymbol(Pair(0,1),BoardState.Symbol.X)
//        ledgerServices.ledger {
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board.writeSymbol(Pair(0,1),BoardState.Symbol.O))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this `fails with` "Only place your symbol on a blank slot"
//            }
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board.writeSymbol(Pair(0,2),BoardState.Symbol.O))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this.verifies()
//            }
//        }
//
//    }
//
//    @Test
//    fun `Player must play only the symbol they were assigned to`() {
//
//    }
//
//    @Test
//    fun `playerX and playerO must alternate turns starting with playerX`() {
//        val board = BoardState(alice.party,bob.party).writeSymbol(Pair(0,1),BoardState.Symbol.X)
//        ledgerServices.ledger {
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board.writeSymbol(Pair(0,2),BoardState.Symbol.X))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this `fails with` "playerX and playerO must alternate turns starting with playerX"
//            }
//            transaction {
//                input(GameContract.ID, board)
//                output(GameContract.ID, board.writeSymbol(Pair(0,2),BoardState.Symbol.O))
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Play())
//                this.verifies()
//            }
//        }
//
//    }
//
//}
//
