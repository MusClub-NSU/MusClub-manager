package com.nsu.musclub;

import com.nsu.musclub.container.PostgresSingletonContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    private static final PostgresSingletonContainer POSTGRES = PostgresSingletonContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Provide default values for DeepSeek API configuration in tests
        registry.add("deepseek.api-url", () -> "https://api.deepseek.com/v1/chat/completions");
        registry.add("deepseek.api-key", () -> System.getenv().getOrDefault("DEEPSEEK_API_KEY", "test-api-key-for-testing-only"));
        registry.add("deepseek.model", () -> "deepseek-chat");
    }
}
