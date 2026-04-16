package com.ktcloud.kcp.cm.adapter.output.persistence.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Connection
import java.util.concurrent.Executors
import javax.sql.DataSource

@Configuration
class DatabasePersistenceConfiguration {

    @Bean("writeHikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.write.hikari")
    fun writeHikariConfig(): HikariConfig = HikariConfig()

    @Bean("readHikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.read.hikari")
    fun readHikariConfig(): HikariConfig = HikariConfig()

    @Bean("writeDataSource")
    fun writeDataSource(
        @Qualifier("writeHikariConfig")
        writeHikariConfig: HikariConfig,
    ): DataSource = HikariDataSource(writeHikariConfig)

    @Bean("readDataSource")
    fun readDataSource(
        @Qualifier("readHikariConfig")
        readHikariConfig: HikariConfig,
    ): DataSource = HikariDataSource(readHikariConfig)

    @Bean("dataSource")
    @Primary
    fun dataSource(
        @Qualifier("writeDataSource")
        writeDataSource: DataSource,
        @Qualifier("readDataSource")
        readDataSource: DataSource,
    ): DataSource = LazyConnectionDataSourceProxy(writeDataSource).apply {
        setReadOnlyDataSource(readDataSource)
        setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    }

    @Bean("databaseCoroutineDispatcher", destroyMethod = "close")
    fun databaseCoroutineDispatcher(): ExecutorCoroutineDispatcher =
        Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

    @Bean("writeTransactionTemplate")
    fun writeTransactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = false
        }

    @Bean("readTransactionTemplate")
    fun readTransactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = true
        }
}
