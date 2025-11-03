package com.nsu.musclub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.user.UserCreateDto;
import com.nsu.musclub.dto.user.UserUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class UserControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_ShouldReturn201() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setRole("MEMBER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldReturn409() throws Exception {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setUsername("duplicate");
        dto1.setEmail("user1@example.com");
        dto1.setRole("MEMBER");

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setUsername("duplicate");
        dto2.setEmail("user2@example.com");
        dto2.setRole("MEMBER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldReturn409() throws Exception {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setUsername("user1");
        dto1.setEmail("duplicate@example.com");
        dto1.setRole("MEMBER");

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setUsername("user2");
        dto2.setEmail("duplicate@example.com");
        dto2.setRole("MEMBER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_WithInvalidData_ShouldReturn400() throws Exception {
        UserCreateDto dto = new UserCreateDto();
        dto.setUsername(""); // Invalid: blank
        dto.setEmail("invalid-email"); // Invalid: not an email
        dto.setRole("MEMBER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_ShouldReturn200() throws Exception {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("getuser");
        createDto.setEmail("getuser@example.com");
        createDto.setRole("MEMBER");

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("getuser"))
                .andExpect(jsonPath("$.email").value("getuser@example.com"));
    }

    @Test
    void getUser_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listUsers_ShouldReturn200() throws Exception {
        // Create multiple users
        for (int i = 1; i <= 3; i++) {
            UserCreateDto dto = new UserCreateDto();
            dto.setUsername("user" + i);
            dto.setEmail("user" + i + "@example.com");
            dto.setRole("MEMBER");
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    void updateUser_ShouldReturn200() throws Exception {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("updateuser");
        createDto.setEmail("updateuser@example.com");
        createDto.setRole("MEMBER");

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("updateduser");
        updateDto.setEmail("updated@example.com");
        updateDto.setRole("ORGANIZER");

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    void updateUser_WithNonExistentId_ShouldReturn404() throws Exception {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("updateduser");
        updateDto.setEmail("updated@example.com");
        updateDto.setRole("MEMBER");

        mockMvc.perform(put("/api/users/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_WithDuplicateUsername_ShouldReturn409() throws Exception {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setUsername("user1");
        dto1.setEmail("user1@example.com");
        dto1.setRole("MEMBER");

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setUsername("user2");
        dto2.setEmail("user2@example.com");
        dto2.setRole("MEMBER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        String response2 = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id2 = objectMapper.readTree(response2).get("id").asLong();

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("user1"); // Duplicate
        updateDto.setEmail("user2@example.com");
        updateDto.setRole("MEMBER");

        mockMvc.perform(put("/api/users/{id}", id2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUser_WithInvalidData_ShouldReturn400() throws Exception {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("invalidtest");
        createDto.setEmail("invalidtest@example.com");
        createDto.setRole("MEMBER");

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername(""); // Invalid
        updateDto.setEmail("invalid-email"); // Invalid
        updateDto.setRole("MEMBER");

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ShouldReturn204() throws Exception {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("deleteuser");
        createDto.setEmail("deleteuser@example.com");
        createDto.setRole("MEMBER");

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
