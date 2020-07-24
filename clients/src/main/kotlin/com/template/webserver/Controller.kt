package com.template.webserver

import com.template.flows.CreateFlow
import com.template.flows.PlayFlow
import com.template.states.BoardState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

val SERVICE_NAMES = listOf("Notary", "Network Map Service") // idk why it's located here

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    @GetMapping(value = [ "activeGames" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getActiveGames() : ResponseEntity<List<StateAndRef<BoardState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<BoardState>().states)
    }

    /**
     * Display all past games this player has played i.e. states that have been taken off ledger
     */
    @GetMapping(value = ["pastGames"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPastStates(): ResponseEntity<List<StateAndRef<BoardState>>> {
        val criteriaByStateStatus = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.CONSUMED)
        return ResponseEntity.ok(proxy.vaultQueryBy<BoardState>(criteriaByStateStatus).states.filter { it.state.data.outcome != "game still in progress" })
    }

    @PostMapping(value = [ "createGame" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createGame(request: HttpServletRequest): ResponseEntity<String> {
        val opponent = request.getParameter("opponent")
        if(opponent == null){
            return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        }
        val partyX500Name = CordaX500Name.parse(opponent)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $opponent cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(::CreateFlow, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @PostMapping(value = [ "play" ], produces = [ TEXT_PLAIN_VALUE ])
    fun play(request: HttpServletRequest): ResponseEntity<String> {
        val opponentString = request.getParameter("opponent")
        val pos = request.getParameter("pos").toInt()
        val opponent = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(opponentString)) ?: throw IllegalArgumentException("Unknown party name.")
        return try {
            proxy.startFlow(::PlayFlow, opponent, pos).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("You played the position ($pos) against ${opponent.name}.")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }


}