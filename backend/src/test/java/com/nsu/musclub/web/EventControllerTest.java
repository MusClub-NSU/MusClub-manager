package com.nsu.musclub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventUpdateDto;
import com.nsu.musclub.dto.event.SocialMediaPostRequestDto;
import com.nsu.musclub.dto.event.SocialMediaPostResponseDto;
import com.nsu.musclub.service.EventPosterAiService;
import com.nsu.musclub.service.SocialMediaPostAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class EventControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventPosterAiService posterAiService;

    @MockBean
    private SocialMediaPostAiService socialMediaPostAiService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        org.mockito.Mockito.reset(posterAiService, socialMediaPostAiService);
    }

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

    @Test
    void listMembers_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Event")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member1\",\"email\":\"member1@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{eventId}/members", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/events/{eventId}/members", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].role").value("ORGANIZER"));
    }

    @Test
    void listMembers_WithNonExistentEvent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}/members", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void upsertMember_ShouldAddMember() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member1\",\"email\":\"member1@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{eventId}/members", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"role\":\"PERFORMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.role").value("PERFORMER"));
    }

    @Test
    void upsertMember_ShouldUpdateMember() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member1\",\"email\":\"member1@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{eventId}/members", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"role\":\"PERFORMER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/events/{eventId}/members", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    void upsertMember_WithCustomRole_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member1\",\"email\":\"member1@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{eventId}/members", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"role\":\"装饰工\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("装饰工"));
    }

    @Test
    void removeMember_ShouldReturn204() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member1\",\"email\":\"member1@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{eventId}/members", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/events/{eventId}/members/{userId}", eventId, userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createSubEvent_ShouldReturn201() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Parent Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long parentId = objectMapper.readTree(eventResponse).get("id").asLong();

        EventCreateDto subEventDto = new EventCreateDto();
        subEventDto.setTitle("Sub Event");
        subEventDto.setStartTime(futureTime().plusDays(2));

        mockMvc.perform(post("/api/events/{parentId}/subevents", parentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Sub Event"));
    }

    @Test
    void attachChild_ShouldReturn204() throws Exception {
        String parentResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Parent"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long parentId = objectMapper.readTree(parentResponse).get("id").asLong();

        String childResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Child"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long childId = objectMapper.readTree(childResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{parentId}/subevents/{childId}", parentId, childId))
                .andExpect(status().isNoContent());
    }

    @Test
    void attachChild_WithCycle_ShouldReturn400() throws Exception {
        String parentResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Parent"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long parentId = objectMapper.readTree(parentResponse).get("id").asLong();

        String childResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Child"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long childId = objectMapper.readTree(childResponse).get("id").asLong();

        mockMvc.perform(post("/api/events/{parentId}/subevents/{childId}", parentId, childId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/events/{parentId}/subevents/{childId}", childId, parentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detachChild_ShouldReturn204() throws Exception {
        String parentResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Parent"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long parentId = objectMapper.readTree(parentResponse).get("id").asLong();

        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child");
        childDto.setStartTime(futureTime().plusDays(2));
        String childResponse = mockMvc.perform(post("/api/events/{parentId}/subevents", parentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long childId = objectMapper.readTree(childResponse).get("id").asLong();

        mockMvc.perform(delete("/api/events/{parentId}/subevents/{childId}", parentId, childId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTree_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Root Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(futureTime().plusDays(2));
        mockMvc.perform(post("/api/events/{parentId}/subevents", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/{eventId}/tree", eventId)
                        .param("depth", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Root Event"))
                .andExpect(jsonPath("$.children").isArray())
                .andExpect(jsonPath("$.children.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void getTree_WithDefaultDepth_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Root Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        mockMvc.perform(get("/api/events/{eventId}/tree", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId));
    }

    private EventCreateDto createEventDto(String title) {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle(title);
        dto.setStartTime(futureTime());
        return dto;
    }

    @Test
    void generatePosterDescription_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String expectedDescription = "Join us for an amazing Test Event!";
        when(posterAiService.generatePosterDescription(eventId, false))
                .thenReturn(expectedDescription);

        mockMvc.perform(post("/api/events/{eventId}/poster-description/ai", eventId)
                        .param("save", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(expectedDescription));
    }

    @Test
    void generatePosterDescription_WithSaveTrue_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        String expectedDescription = "Amazing event description";
        when(posterAiService.generatePosterDescription(eventId, true))
                .thenReturn(expectedDescription);

        mockMvc.perform(post("/api/events/{eventId}/poster-description/ai", eventId)
                        .param("save", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(expectedDescription));
    }

    @Test
    void generatePosterDescription_WithNonExistentEvent_ShouldReturn404() throws Exception {
        when(posterAiService.generatePosterDescription(99999L, false))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));

        mockMvc.perform(post("/api/events/{eventId}/poster-description/ai", 99999L)
                        .param("save", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateSocialMediaPost_WithDefaultParams_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        SocialMediaPostResponseDto expectedResponse = new SocialMediaPostResponseDto(
                "Join us for Test Event! #Music", "general", "casual");
        when(socialMediaPostAiService.generateSocialMediaPost(eventId, "general", "casual"))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/events/{eventId}/social-media-post/ai", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Join us for Test Event! #Music"))
                .andExpect(jsonPath("$.platform").value("general"))
                .andExpect(jsonPath("$.tone").value("casual"));
    }

    @Test
    void generateSocialMediaPost_WithPlatformAndTone_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        SocialMediaPostResponseDto expectedResponse = new SocialMediaPostResponseDto(
                "Rock Concert 2024! 🎸 #RockConcert", "twitter", "enthusiastic");
        when(socialMediaPostAiService.generateSocialMediaPost(eventId, "twitter", "enthusiastic"))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/events/{eventId}/social-media-post/ai", eventId)
                        .param("platform", "twitter")
                        .param("tone", "enthusiastic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Rock Concert 2024! 🎸 #RockConcert"))
                .andExpect(jsonPath("$.platform").value("twitter"))
                .andExpect(jsonPath("$.tone").value("enthusiastic"));
    }

    @Test
    void generateSocialMediaPost_WithInstagram_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        SocialMediaPostResponseDto expectedResponse = new SocialMediaPostResponseDto(
                "Amazing event! 🎵✨\n#Music #Event", "instagram", "casual");
        when(socialMediaPostAiService.generateSocialMediaPost(eventId, "instagram", "casual"))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/events/{eventId}/social-media-post/ai", eventId)
                        .param("platform", "instagram")
                        .param("tone", "casual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("instagram"));
    }

    @Test
    void generateSocialMediaPost_WithAdvancedEndpoint_ShouldReturn200() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        SocialMediaPostRequestDto requestDto = new SocialMediaPostRequestDto();
        requestDto.setPlatform("facebook");
        requestDto.setTone("professional");

        SocialMediaPostResponseDto expectedResponse = new SocialMediaPostResponseDto(
                "Professional event announcement", "facebook", "professional");
        when(socialMediaPostAiService.generateSocialMediaPost(eventId, "facebook", "professional"))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/events/{eventId}/social-media-post/ai/advanced", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("facebook"))
                .andExpect(jsonPath("$.tone").value("professional"));
    }

    @Test
    void generateSocialMediaPost_WithAdvancedEndpoint_WithoutBody_ShouldUseDefaults() throws Exception {
        String eventResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEventDto("Test Event"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        SocialMediaPostResponseDto expectedResponse = new SocialMediaPostResponseDto(
                "Default post", "general", "casual");
        when(socialMediaPostAiService.generateSocialMediaPost(eventId))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/events/{eventId}/social-media-post/ai/advanced", eventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("general"))
                .andExpect(jsonPath("$.tone").value("casual"));
    }

    @Test
    void generateSocialMediaPost_WithNonExistentEvent_ShouldReturn404() throws Exception {
        when(socialMediaPostAiService.generateSocialMediaPost(99999L, "general", "casual"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));

        mockMvc.perform(post("/api/events/{eventId}/social-media-post/ai", 99999L))
                .andExpect(status().isNotFound());
    }
}
