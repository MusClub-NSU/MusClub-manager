package com.nsu.musclub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.user.UserCreateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class SearchControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void hybridSearch_ByEventQuery_ShouldReturnEvent() throws Exception {
        EventCreateDto event = new EventCreateDto();
        event.setTitle("Jazz Night Novosibirsk");
        event.setDescription("Live saxophone and piano session");
        event.setStartTime(OffsetDateTime.now().plusDays(2));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/search/hybrid")
                        .param("q", "jazz night")
                        .param("types", "EVENT")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].entityType", is("EVENT")));
    }

    @Test
    void hybridSearch_ByUserQuery_ShouldReturnUser() throws Exception {
        UserCreateDto user = new UserCreateDto();
        user.setUsername("guitar_hero");
        user.setEmail("guitar.hero@example.com");
        user.setRole("MEMBER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/search/hybrid")
                        .param("q", "guitar hero")
                        .param("types", "USER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].entityType", is("USER")));
    }

    @Test
    void hybridSearch_EmptyQuery_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/search/hybrid")
                        .param("q", "   ")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }
}

