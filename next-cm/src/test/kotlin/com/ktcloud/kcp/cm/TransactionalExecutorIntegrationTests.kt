package com.ktcloud.kcp.cm

import com.ktcloud.kcp.cm.application.port.output.transaction.TransactionalPort
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class TransactionalExecutorIntegrationTests(@Autowired private val transactionalPort: TransactionalPort) {
    @Test
    fun `write transaction block runs on virtual thread`() = runBlocking {
        val isVirtualThread = transactionalPort.execute {
            Thread.currentThread().isVirtual
        }

        assertTrue(isVirtualThread)
    }

    @Test
    fun `read only transaction block runs on virtual thread`() = runBlocking {
        val isVirtualThread = transactionalPort.executeReadOnly {
            Thread.currentThread().isVirtual
        }

        assertTrue(isVirtualThread)
    }
}
