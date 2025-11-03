package com.nsu.musclub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class EventControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OffsetDateTime futureTime() {
        return OffsetDateTime.now().plusDays(1);
    }

    private OffsetDateTime pastTime() {
        return OffsetDateTime.now().minusDays(1);
    }

    @Test
    void createEvent_ShouldReturn201() throws Exception {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Test Event");
        dto.setDescription("Test Description");
        dto.setStartTime(futureTime());
        dto.setEndTime(futureTime().plusHours(2));
        dto.setVenue("Test Venue");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.venue").value("Test Venue"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createEvent_WithPastStartTime_ShouldReturn400() throws Exception {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Past Event");
        dto.setStartTime(pastTime());
        dto.setVenue("Test Venue");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_WithEndTimeBeforeStartTime_ShouldReturn400() throws Exception {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Invalid Event");
        dto.setStartTime(futureTime());
        dto.setEndTime(futureTime().minusHours(2)); // End before start

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_WithEndTimeEqualToStartTime_ShouldReturn201() throws Exception {
        OffsetDateTime start = futureTime();
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Valid Event");
        dto.setStartTime(start);
        dto.setEndTime(start); // Equal to start time is valid

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void createEvent_WithInvalidData_ShouldReturn400() throws Exception {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle(""); // Invalid: blank

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvent_ShouldReturn200() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Get Event");
        createDto.setStartTime(futureTime());
        createDto.setVenue("Test Venue");

        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Get Event"))
                .andExpect(jsonPath("$.venue").value("Test Venue"));
    }

    @Test
    void getEvent_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/events/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listEvents_ShouldReturn200() throws Exception {
        // Create multiple events
        for (int i = 1; i <= 3; i++) {
            EventCreateDto dto = new EventCreateDto();
            dto.setTitle("Event " + i);
            dto.setStartTime(futureTime().plusDays(i));
            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    void updateEvent_ShouldReturn200() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Title");
        createDto.setStartTime(futureTime());
        createDto.setVenue("Original Venue");

        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Title");
        updateDto.setDescription("Updated Description");
        updateDto.setStartTime(futureTime().plusDays(2));
        updateDto.setEndTime(futureTime().plusDays(2).plusHours(3));
        updateDto.setVenue("Updated Venue");

        mockMvc.perform(put("/api/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.venue").value("Updated Venue"));
    }

    @Test
    void updateEvent_WithNonExistentId_ShouldReturn404() throws Exception {
        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Title");
        updateDto.setStartTime(futureTime());

        mockMvc.perform(put("/api/events/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEvent_WithPastStartTime_ShouldReturn400() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Event");
        createDto.setStartTime(futureTime());

        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Event");
        updateDto.setStartTime(pastTime());

        mockMvc.perform(put("/api/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_WithEndTimeBeforeStartTime_ShouldReturn400() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Event");
        createDto.setStartTime(futureTime());

        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Event");
        updateDto.setStartTime(futureTime());
        updateDto.setEndTime(futureTime().minusHours(1)); // Invalid: end before start

        mockMvc.perform(put("/api/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_WithInvalidData_ShouldReturn400() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Event");
        createDto.setStartTime(futureTime());

        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle(""); // Invalid: blank

        mockMvc.perform(put("/api/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEvent_ShouldReturn204() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Delete Event");
        createDto.setStartTime(futureTime());

        String response = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/events/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/events/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEvent_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/events/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
