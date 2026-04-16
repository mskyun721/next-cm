package com.ktcloud.kcp.cm

import com.ktcloud.kcp.cm.common.constant.CommonConstant.API_VERSION_V1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReadWriteDatabaseRoutingIntegrationTests {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("writeDataSource")
    private lateinit var writeDataSource: DataSource

    @Autowired
    @Qualifier("readDataSource")
    private lateinit var readDataSource: DataSource

    private lateinit var webTestClient: WebTestClient
    private lateinit var writeJdbcTemplate: JdbcTemplate
    private lateinit var readJdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://127.0.0.1:$port")
            .build()

        writeJdbcTemplate = JdbcTemplate(writeDataSource)
        readJdbcTemplate = JdbcTemplate(readDataSource)

        writeJdbcTemplate.update("DELETE FROM samples")
        readJdbcTemplate.update("DELETE FROM samples")

        writeJdbcTemplate.update("INSERT INTO samples (name, age, status) VALUES ('writer-only', 11, 'active')")
        readJdbcTemplate.update("INSERT INTO samples (name, age, status) VALUES ('reader-only', 22, 'active')")
    }

    @Test
    fun `GET samples reads from read database`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$[0].name").isEqualTo("reader-only")
            .jsonPath("$[0].age").isEqualTo(22)
            .jsonPath("$[1]").doesNotExist()
    }
}
