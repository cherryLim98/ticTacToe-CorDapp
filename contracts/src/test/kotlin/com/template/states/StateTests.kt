package com.template.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Test

class StateTests {
    private val alice = TestIdentity(CordaX500Name("Alice","London","GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob","New York","USA")).party

    /**
     *
     */
    @Test
    fun ``() {

    }
}