package com.ktcloud.kcp.cm

import com.ktcloud.kcp.cm.common.constant.CommonConstant.API_VERSION_V1
import com.ktcloud.kcp.cm.common.errors.CommonErrorCode
import com.ktcloud.kcp.cm.common.errors.ErrorSource
import com.ktcloud.kcp.cm.common.errors.SampleErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationHttpIntegrationTests {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    @Qualifier("writeDataSource")
    private lateinit var writeDataSource: DataSource

    @Autowired
    @Qualifier("readDataSource")
    private lateinit var readDataSource: DataSource

    private lateinit var webTestClient: WebTestClient
    private lateinit var writeJdbc: JdbcTemplate
    private lateinit var readJdbc: JdbcTemplate

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://127.0.0.1:$port")
            .build()
        writeJdbc = JdbcTemplate(writeDataSource)
        readJdbc = JdbcTemplate(readDataSource)
        writeJdbc.update("DELETE FROM samples")
        readJdbc.update("DELETE FROM samples")
    }

    // ── CRUD 정상 흐름 ──────────────────────────────────────────────────────

    @Test
    fun `POST creates a sample and returns it`() {
        webTestClient.post()
            .uri("/sample/$API_VERSION_V1/samples")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"Alice","age":30,"status":"ACTIVE"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isNotEmpty
            .jsonPath("$.name").isEqualTo("Alice")
            .jsonPath("$.age").isEqualTo(30)
            .jsonPath("$.status").isEqualTo("ACTIVE")
    }

    @Test
    fun `GET by id returns existing sample`() {
        // GET 은 read DB 에서 조회
        readJdbc.update("INSERT INTO samples (id, name, age, status) VALUES (1, 'Bob', 25, 'active')")

        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.name").isEqualTo("Bob")
            .jsonPath("$.age").isEqualTo(25)
    }

    @Test
    fun `GET search with query params filters results`() {
        // GET 은 read DB 에서 조회
        readJdbc.update("INSERT INTO samples (name, age, status) VALUES ('Alice', 30, 'active')")
        readJdbc.update("INSERT INTO samples (name, age, status) VALUES ('Bob', 20, 'inactive')")
        readJdbc.update("INSERT INTO samples (name, age, status) VALUES ('Charlie', 40, 'active')")

        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples?minAge=25&maxAge=35")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$[0].name").isEqualTo("Alice")
            .jsonPath("$[1]").doesNotExist()
    }

    @Test
    fun `PUT updates an existing sample`() {
        // PUT 은 write DB 에서 수정
        writeJdbc.update("INSERT INTO samples (id, name, age, status) VALUES (1, 'Old', 10, 'active')")

        webTestClient.put()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .header("X-Modified-By", "tester")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"Updated","age":99,"status":"INACTIVE"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.name").isEqualTo("Updated")
            .jsonPath("$.age").isEqualTo(99)
    }

    @Test
    fun `DELETE removes an existing sample`() {
        // DELETE 는 write DB 에서 삭제
        writeJdbc.update("INSERT INTO samples (id, name, age, status) VALUES (1, 'ToDelete', 1, 'active')")

        webTestClient.delete()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .exchange()
            .expectStatus().isNoContent
    }

    // ── 에러 응답 ───────────────────────────────────────────────────────────

    @Test
    fun `GET by id returns 404 when sample not found`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples/999")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().exists("X-Trace-Id")
            .expectBody()
            .jsonPath("$.code").isEqualTo(SampleErrorCode.SAMPLE_NOT_FOUND.code)
            .jsonPath("$.traceId").exists()
    }

    @Test
    fun `PUT returns 400 when X-Modified-By header is missing`() {
        webTestClient.put()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"Test","age":20,"status":"ACTIVE"}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo(CommonErrorCode.INVALID_HEADER_PARAMETER.code)
            .jsonPath("$.errors[0].source").isEqualTo(ErrorSource.HEADER.wireName)
            .jsonPath("$.errors[0].field").isEqualTo("X-Modified-By")
    }

    @Test
    fun `PUT returns 400 when body is empty`() {
        webTestClient.put()
            .uri("/sample/$API_VERSION_V1/samples/1")
            .header("X-Modified-By", "tester")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo(CommonErrorCode.EMPTY_BODY.code)
    }

    @Test
    fun `POST returns validation errors when body is invalid`() {
        webTestClient.post()
            .uri("/sample/$API_VERSION_V1/samples")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name":"","age":-1,"status":"ACTIVE"}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo(CommonErrorCode.VALIDATION_FAIL.code)
            .jsonPath("$.errors").isArray
            .jsonPath("$.errors[0].source").isEqualTo(ErrorSource.BODY.wireName)
    }

    @Test
    fun `DELETE returns 404 when sample not found`() {
        webTestClient.delete()
            .uri("/sample/$API_VERSION_V1/samples/999")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.code").isEqualTo(SampleErrorCode.SAMPLE_NOT_FOUND.code)
    }

    @Test
    fun `GET returns 400 for invalid path variable`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples/abc")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code)
    }

    @Test
    fun `GET returns query field name when query parameter binding fails`() {
        webTestClient.get()
            .uri("/sample/$API_VERSION_V1/samples?minAge=abc")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code)
            .jsonPath("$.errors[0].source").isEqualTo(ErrorSource.QUERY.wireName)
            .jsonPath("$.errors[0].field").isEqualTo("minAge")
    }

    // ── 공통 에러 ───────────────────────────────────────────────────────────

    @Test
    fun `unknown path returns not found`() {
        webTestClient.get()
            .uri("/unknown")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().exists("X-Trace-Id")
            .expectBody()
            .jsonPath("$.code").isEqualTo(CommonErrorCode.NOT_FOUND.code)
            .jsonPath("$.traceId").exists()
    }

    @Test
    fun `provided trace id is reused in response`() {
        val traceId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        webTestClient.get()
            .uri("/unknown")
            .header("X-Trace-Id", traceId)
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().valueEquals("X-Trace-Id", traceId)
            .expectBody()
            .jsonPath("$.traceId").isEqualTo(traceId)
    }
}
