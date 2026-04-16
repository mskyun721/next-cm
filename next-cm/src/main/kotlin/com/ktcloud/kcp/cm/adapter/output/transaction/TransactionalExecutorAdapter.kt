package com.ktcloud.kcp.cm.adapter.output.transaction

import com.ktcloud.kcp.cm.application.port.output.transaction.TransactionalPort
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import kotlin.coroutines.CoroutineContext

@Component
class TransactionalExecutorAdapter(
    @Qualifier("databaseCoroutineDispatcher")
    private val databaseCoroutineDispatcher: CoroutineContext,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate,
    @Qualifier("readTransactionTemplate")
    private val readTransactionTemplate: TransactionTemplate,
) : TransactionalPort {
    private val log = LoggerFactory.getLogger(this::class.java)

    private object UninitializedResult

    override suspend fun <T> execute(block: () -> T): T = runInTransaction(
        role = "write",
        transactionTemplate = writeTransactionTemplate,
        readOnly = false,
        block = block,
    )

    override suspend fun <T> executeReadOnly(block: () -> T): T = runInTransaction(
        role = "read",
        transactionTemplate = readTransactionTemplate,
        readOnly = true,
        block = block,
    )

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> runInTransaction(
        role: String,
        transactionTemplate: TransactionTemplate,
        readOnly: Boolean,
        block: () -> T,
    ): T = withContext(databaseCoroutineDispatcher) {
        // Spring JDBC transaction state is thread-bound, so the whole callback must stay on one virtual thread.
        log.debug(
            "runInTransaction() - role={}, readOnly={}, thread={}, virtual={}",
            role,
            readOnly,
            Thread.currentThread().name,
            Thread.currentThread().isVirtual,
        )

        var result: Any? = UninitializedResult
        transactionTemplate.execute {
            result = block()
        }

        result as T
    }
}
