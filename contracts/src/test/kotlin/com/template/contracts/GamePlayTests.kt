package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Test

/**
 * Tests for Play Command
 */
class GamePlayTests {
    class DummyCommand : TypeOnlyCommandData()

    private val ledgerServices = MockServices()
    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB"))
    private val bob = TestIdentity(CordaX500Name("Bob", "London", "GB"))
    private val carl = TestIdentity(CordaX500Name("Carl", "New York", "US"))

    /**
     * Players cannot erase a nonblank symbol. They also must put a nonblank symbol down
     */
    @Test
    fun `The U Symbol cannot be played`() {
        val board = BoardState(alice.party,bob.party)

    }

    @Test
    fun `The number of blanks must decrease`() {

    }

}
