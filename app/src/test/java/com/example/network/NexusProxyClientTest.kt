package com.example.network

import kotlinx.coroutines.runBlocking
import com.example.network.NexusProxyClient
import org.junit.Test

class NexusProxyClientTest {
    @Test
    fun testScrape() = runBlocking {
        val client = NexusProxyClient()
        val result = client.buscarDadosAtivo("PETR4")
        println("PETR4 RESULT: $result")
    }
}
