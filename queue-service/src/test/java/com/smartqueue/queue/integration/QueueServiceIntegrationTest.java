package com.smartqueue.queue.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqueue.queue.dto.BookTokenRequest;
import com.smartqueue.queue.entity.ServiceEntity;
import com.smartqueue.queue.repository.ServiceRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueueServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("queue_db_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> ""); // no auth for test container
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private StringRedisTemplate redisTemplate;

    private static Long serviceId;
    private static Long tokenId;

    @BeforeAll
    static void seedService(@Autowired ServiceRepository serviceRepository) {
        ServiceEntity service = ServiceEntity.builder()
                .name("Test Service")
                .description("Integration test service")
                .isActive(true)
                .avgServiceTimeSeconds(120)
                .maxDailyTokens(100)
                .build();
        serviceId = serviceRepository.save(service).getId();
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/services - should return active services")
    void shouldReturnActiveServices() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/queues/{id}/book - should book a token")
    void shouldBookToken() throws Exception {
        BookTokenRequest request = new BookTokenRequest();
        request.setUserPhone("+919876543210");

        MvcResult result = mockMvc.perform(post("/api/queues/" + serviceId + "/book")
                        .header("X-User-Id", "100")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenNumber").exists())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.queuePosition").value(1))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
        tokenId = root.path("data").path("tokenId").asLong();

        // Verify Redis ZSET was populated
        Long size = redisTemplate.opsForZSet().zCard("queue:" + serviceId);
        assertThat(size).isEqualTo(1L);
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/queues/{id}/book - should reject duplicate active token")
    void shouldRejectDuplicateActiveToken() throws Exception {
        BookTokenRequest request = new BookTokenRequest();
        request.setUserPhone("+919876543210");

        mockMvc.perform(post("/api/queues/" + serviceId + "/book")
                        .header("X-User-Id", "100") // same userId
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("QUEUE_004"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/queues/{id}/status - should return queue status with user token")
    void shouldReturnQueueStatusWithUserToken() throws Exception {
        mockMvc.perform(get("/api/queues/" + serviceId + "/status")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalWaiting").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.myToken").exists())
                .andExpect(jsonPath("$.data.myToken.status").value("WAITING"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/tokens/{id} - should return token details for owner")
    void shouldReturnTokenDetailsForOwner() throws Exception {
        Assumptions.assumeTrue(tokenId != null, "tokenId must be set");

        mockMvc.perform(get("/api/tokens/" + tokenId)
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/tokens/{id} - should reject access by different user")
    void shouldRejectTokenAccessByDifferentUser() throws Exception {
        Assumptions.assumeTrue(tokenId != null, "tokenId must be set");

        mockMvc.perform(get("/api/tokens/" + tokenId)
                        .header("X-User-Id", "999")) // different user
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_007"));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/tokens/{id}/cancel - should cancel WAITING token")
    void shouldCancelWaitingToken() throws Exception {
        Assumptions.assumeTrue(tokenId != null, "tokenId must be set");

        mockMvc.perform(put("/api/tokens/" + tokenId + "/cancel")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Verify Redis ZSET was updated (token removed)
        Long size = redisTemplate.opsForZSet().zCard("queue:" + serviceId);
        assertThat(size).isEqualTo(0L);
    }
}
