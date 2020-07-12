package com.template

import com.template.flows.CreateFlow
import com.template.flows.CreateResponder
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
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(CreateResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `flow records the same tx in both party vaults`() {
        val bob = b.info.chooseIdentityAndCert().party
        val flow = CreateFlow(bob)
        val future = a.startFlow(flow)
        network.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }

    @Test
    fun `if an ongoing game with chosen player already exists, you cannot create a new state with that player`() {
        val bob = b.info.chooseIdentityAndCert().party
        a.startFlow(CreateFlow(bob))
        network.runNetwork()
        val future = a.startFlow(CreateFlow(bob))
        network.runNetwork() // need to run network before getting the future
        assertFailsWith<IllegalArgumentException>("You can only have one ongoing game with your chosen player at a time") { future.getOrThrow() }
    }
}