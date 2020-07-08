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
///**
// * Tests for Create Command
// */
//class GameCreateTests {
//    class DummyCommand : TypeOnlyCommandData()
//    private val ledgerServices = MockServices()
//    private val alice = TestIdentity(CordaX500Name("Alice","London","GB"))
//    private val bob = TestIdentity(CordaX500Name("Bob","London","GB"))
//    private val carl = TestIdentity(CordaX500Name("Carl","New York","US"))
//
//    @Test
//    fun `Create transactions are accepted, but transactions not defined in our commands are failed`() {
//        val board = BoardState(alice.party,bob.party)
//        ledgerServices.ledger {
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey,bob.publicKey), DummyCommand())
//                this.fails()
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey,bob.publicKey), GameContract.Commands.Create())
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `Create transaction must have no inputs`() {
//        val board = BoardState(alice.party, bob.party)
//        ledgerServices.ledger {
//            transaction {
//                input(GameContract.ID, DummyState())
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this `fails with` "No input states should be consumed when creating a game"
//            }
//        }
//    }
//
//    @Test
//    fun `Create transaction must have only one output`() {
//        val board = BoardState(alice.party, bob.party)
//        ledgerServices.ledger {
//            transaction {
//                output(GameContract.ID, board)
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this `fails with` "There should be just one output state in game creation"
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `The two players on the board cannot be the same person`() {
//        val board = BoardState(alice.party, bob.party)
//        val invalidBoard = BoardState(alice.party, alice.party)
//        ledgerServices.ledger {
//            transaction {
//                output(GameContract.ID, invalidBoard)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this `fails with` "The players on the board must be different identities"
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `Create transaction signers must be both the players on the board and only them`() {
//        val board = BoardState(alice.party, bob.party)
//        ledgerServices.ledger {
//            transaction {
//                output(GameContract.ID, board)
//                command(alice.publicKey, GameContract.Commands.Create())
//                this `fails with` "Create transaction signers must be both the players on the board and only them"
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(bob.publicKey, GameContract.Commands.Create())
//                this `fails with` "Create transaction signers must be both the players on the board and only them"
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, carl.publicKey), GameContract.Commands.Create())
//                this `fails with` "Create transaction signers must be both the players on the board and only them"
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this.verifies()
//            }
//            transaction {
//                output(GameContract.ID, board)
//                command(listOf(bob.publicKey, alice.publicKey, bob.publicKey), GameContract.Commands.Create())
//                this.verifies()
//            }
//        }
//    }
//}