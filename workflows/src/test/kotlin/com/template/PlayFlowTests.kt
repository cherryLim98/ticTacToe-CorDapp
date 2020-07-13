package com.template

import com.template.flows.CreateFlow
import com.template.flows.CreateResponder
import com.template.flows.PlayFlow
import com.template.flows.PlayResponder
import com.template.states.BoardState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(CreateResponder::class.java)
            it.registerInitiatedFlow(PlayResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * create game
     */
    private fun aliceCreateGame(party: Party): SignedTransaction {
        val flow = CreateFlow(party)
        val future = a.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    private fun executePlayTransaction(inTx: SignedTransaction, pos: Pair<Int,Int>, node: StartedMockNode) : SignedTransaction {
        val input = inTx.tx.outputs.single().data as BoardState
        val flow = PlayFlow((input.participants-node.info.chooseIdentity()).single() as Party, pos.first, pos.second)
        val future = node.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    @Test
    fun `non-participant cannot play a move`() {
        val bob = b.info.chooseIdentityAndCert().party
        aliceCreateGame(bob)
        val flow = PlayFlow(bob, 0, 1)
        val future = c.startFlow(flow)
        network.runNetwork()
        assertFailsWith<NoSuchElementException> { future.getOrThrow() } // actually fails with NoSuchElementException cuz the state doesn't exits in c's vault
    }

    @Test
    fun `same player cannot take two turns in a row`() {
        val bob = b.info.chooseIdentityAndCert().party
        // first turn
        aliceCreateGame(bob)
        val flow = PlayFlow(bob, 0, 1)
        val future = a.startFlow(flow)
        network.runNetwork()
        // second turn
        assertFailsWith<IllegalArgumentException>("The two players must alternate turns i.e. the same player cannot play twice in a row"){ executePlayTransaction(future.getOrThrow(), Pair(0,2), a) }
    }

    @Test
    fun `players must place a symbol on blank slots only`() {
        val bob = b.info.chooseIdentityAndCert().party
        // first turn
        aliceCreateGame(bob)
        val flow = PlayFlow(bob, 0, 1)
        val future = a.startFlow(flow)
        network.runNetwork()
        // second turn
        assertFailsWith<TransactionVerificationException>("You must place a symbol on a blank slot") { executePlayTransaction(future.getOrThrow(), Pair(0,1), b) }
    }

    @Test
    fun `State is removed from ledger and made historical for both parties when somebody wins`() {
        val bob = b.info.chooseIdentityAndCert().party
        val stx = aliceCreateGame(bob)
        val stxB = executePlayTransaction(stx, Pair(0,0), a)
        val stxC = executePlayTransaction(stxB, Pair(1,1), b)
        val stxD = executePlayTransaction(stxC, Pair(0,1), a)
        val stxE = executePlayTransaction(stxD, Pair(1,2), b)
        val stxF = executePlayTransaction(stxE, Pair(0,2), a)
        // has the state chain ended for both parties
        val id = (stxF.tx.outputs.single().data as BoardState).linearId
        val criteriaById = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        assertFailsWith<NoSuchElementException>{a.services.vaultService.queryBy<BoardState>(criteriaById).states.single()}
        assertFailsWith<NoSuchElementException>{b.services.vaultService.queryBy<BoardState>(criteriaById).states.single()}
        // is there a correct number of consumed states for both parties
        val criteriaByStateStatus = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.CONSUMED)
        assert(a.services.vaultService.queryBy<BoardState>(criteriaByStateStatus).states.size == 6 )
        assert(b.services.vaultService.queryBy<BoardState>(criteriaByStateStatus).states.size == 6 )
    }

    @Test
    fun `Final state records the correct outcome if somebody wins`() {
        val bob = b.info.chooseIdentityAndCert().party
        val stx = aliceCreateGame(bob)
        val stxB = executePlayTransaction(stx, Pair(0,0), a)
        val stxC = executePlayTransaction(stxB, Pair(1,1), b)
        val stxD = executePlayTransaction(stxC, Pair(0,1), a)
        val stxE = executePlayTransaction(stxD, Pair(1,2), b)
        val stxF = executePlayTransaction(stxE, Pair(0,2), a)
        val outcome = (stxF.tx.outputs.single().data as BoardState).outcome
        assertEquals("${a.info.chooseIdentity().name.commonName} wins", outcome)
    }

    /**
     * b a a
     * a b b
     * a b a
     */
    @Test
    fun `Final state records the correct outcome if there is a draw`() {
        val bob = b.info.chooseIdentityAndCert().party
        val stx = aliceCreateGame(bob)
        val stxB = executePlayTransaction(stx, Pair(0,0), a)
        val stxC = executePlayTransaction(stxB, Pair(1,0), b)
        val stxD = executePlayTransaction(stxC, Pair(2,0), a)
        val stxE = executePlayTransaction(stxD, Pair(1,1), b)
        val stxF = executePlayTransaction(stxE, Pair(0,1), a)
        val stxG = executePlayTransaction(stxF, Pair(2,1), b)
        val stxH = executePlayTransaction(stxG, Pair(1,2), a)
        val stxI = executePlayTransaction(stxH, Pair(0,2), b)
        val stxJ = executePlayTransaction(stxI, Pair(2,2), a)

        val outcome = (stxJ.tx.outputs.single().data as BoardState).outcome
        assertEquals("Draw", outcome)
    }

    /**
     * kind of redundant test because a game that has been won would have been removed from ledger so this is just the same as the "removed from ledger" test
     */
    @Test
    fun `You cannot play on a game that has been won or reached a draw`() {
        val bob = b.info.chooseIdentityAndCert().party
        val stx = aliceCreateGame(bob)
        val stxB = executePlayTransaction(stx, Pair(0,0), a)
        val stxC = executePlayTransaction(stxB, Pair(1,1), b)
        val stxD = executePlayTransaction(stxC, Pair(0,1), a)
        val stxE = executePlayTransaction(stxD, Pair(1,2), b)
        val stxF = executePlayTransaction(stxE, Pair(0,2), a)
        // by this point alice has won
        assertFailsWith<NoSuchElementException> { executePlayTransaction(stxF, Pair(1,0), b) }
    }

}