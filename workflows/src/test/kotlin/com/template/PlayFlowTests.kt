package com.template

import com.template.flows.CreateFlow
import com.template.flows.PlayFlow
import com.template.flows.PlayResponder
import com.template.states.BoardState
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
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
    private fun createGame(state: BoardState): SignedTransaction {
        val flow = CreateFlow(state)
        val future = a.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    @Test
    fun `non-participant cannot play a move`() {
        val alice = a.info.chooseIdentityAndCert().party
        val bob = b.info.chooseIdentityAndCert().party
        val stx = createGame(BoardState(alice,bob))
        val input = stx.tx.outputs.single().data as BoardState


        val flow = PlayFlow(input.linearId, Pair(0,1))
        val future = c.startFlow(flow)
        network.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }

    }

    @Test
    fun `same player cannot take two turns in a row`() {
        val alice = a.info.chooseIdentityAndCert().party
        val bob = b.info.chooseIdentityAndCert().party
        // first turn
        val stx = createGame(BoardState(alice,bob))
        val input = stx.tx.outputs.single().data as BoardState
        val flow = PlayFlow(input.linearId, Pair(0,1))
        val future = a.startFlow(flow)
        network.runNetwork()
        // second turn
        val stxB = future.getOrThrow()
        val inputB = stxB.tx.outputs.single().data as BoardState
        val flowB = PlayFlow(inputB.linearId, Pair(0,1))
        val futureB = a.startFlow(flowB)
        network.runNetwork()
        assertFailsWith<IllegalArgumentException> { futureB.getOrThrow() }
    }
}