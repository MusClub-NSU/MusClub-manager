package com.nsu.musclub.service;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventResponseDto;
import com.nsu.musclub.dto.event.EventUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.transaction.annotation.Transactional
class EventServiceTest extends AbstractIntegrationTest {

    @Autowired
    private EventService eventService;

    private OffsetDateTime futureTime() {
        return OffsetDateTime.now().plusDays(1);
    }

    private OffsetDateTime pastTime() {
        return OffsetDateTime.now().minusDays(1);
    }

    @Test
    void createEvent_ShouldSucceed() {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Test Event");
        dto.setDescription("Test Description");
        dto.setStartTime(futureTime());
        dto.setEndTime(futureTime().plusHours(2));
        dto.setVenue("Test Venue");

        EventResponseDto result = eventService.create(dto);

        assertNotNull(result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals("Test Venue", result.getVenue());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createEvent_WithPastStartTime_ShouldThrow400() {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Past Event");
        dto.setStartTime(pastTime());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.create(dto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Start time must be in the future"));
    }

    @Test
    void createEvent_WithEndTimeBeforeStartTime_ShouldThrow400() {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Invalid Event");
        dto.setStartTime(futureTime());
        dto.setEndTime(futureTime().minusHours(2));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.create(dto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("End time must be greater than or equal to start time"));
    }

    @Test
    void createEvent_WithEndTimeEqualToStartTime_ShouldSucceed() {
        OffsetDateTime start = futureTime();
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Valid Event");
        dto.setStartTime(start);
        dto.setEndTime(start); // Equal is valid

        EventResponseDto result = eventService.create(dto);

        assertNotNull(result.getId());
        assertEquals(start, result.getStartTime());
        assertEquals(start, result.getEndTime());
    }

    @Test
    void createEvent_WithoutEndTime_ShouldSucceed() {
        EventCreateDto dto = new EventCreateDto();
        dto.setTitle("Event Without End");
        dto.setStartTime(futureTime());
        dto.setEndTime(null); // End time is optional

        EventResponseDto result = eventService.create(dto);

        assertNotNull(result.getId());
        assertNull(result.getEndTime());
    }

    @Test
    void getEvent_ShouldReturnEvent() {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Get Event");
        createDto.setStartTime(futureTime());

        EventResponseDto created = eventService.create(createDto);
        EventResponseDto found = eventService.get(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Get Event", found.getTitle());
    }

    @Test
    void getEvent_WithNonExistentId_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.get(99999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void listEvents_ShouldReturnPage() {
        // Create multiple events
        for (int i = 1; i <= 5; i++) {
            EventCreateDto dto = new EventCreateDto();
            dto.setTitle("Event " + i);
            dto.setStartTime(futureTime().plusDays(i));
            eventService.create(dto);
        }

        Page<EventResponseDto> page = eventService.list(Pageable.ofSize(10));

        assertTrue(page.getTotalElements() >= 5);
        assertEquals(10, page.getSize());
    }

    @Test
    void updateEvent_ShouldSucceed() {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Title");
        createDto.setStartTime(futureTime());
        createDto.setVenue("Original Venue");

        EventResponseDto created = eventService.create(createDto);

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Title");
        updateDto.setDescription("Updated Description");
        updateDto.setStartTime(futureTime().plusDays(2));
        updateDto.setEndTime(futureTime().plusDays(2).plusHours(3));
        updateDto.setVenue("Updated Venue");

        EventResponseDto updated = eventService.update(created.getId(), updateDto);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals("Updated Venue", updated.getVenue());
    }

    @Test
    void updateEvent_WithNonExistentId_ShouldThrow404() {
        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Title");
        updateDto.setStartTime(futureTime());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.update(99999L, updateDto);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateEvent_WithPastStartTime_ShouldThrow400() {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Event");
        createDto.setStartTime(futureTime());

        EventResponseDto created = eventService.create(createDto);

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Event");
        updateDto.setStartTime(pastTime());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.update(created.getId(), updateDto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Start time must be in the future"));
    }

    @Test
    void updateEvent_WithEndTimeBeforeStartTime_ShouldThrow400() {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Original Event");
        createDto.setStartTime(futureTime());

        EventResponseDto created = eventService.create(createDto);

        EventUpdateDto updateDto = new EventUpdateDto();
        updateDto.setTitle("Updated Event");
        updateDto.setStartTime(futureTime());
        updateDto.setEndTime(futureTime().minusHours(1)); // Invalid

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.update(created.getId(), updateDto);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("End time must be greater than or equal to start time"));
    }

    @Test
    void deleteEvent_ShouldSucceed() {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Delete Event");
        createDto.setStartTime(futureTime());

        EventResponseDto created = eventService.create(createDto);
        Long id = created.getId();

        eventService.delete(id);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.get(id);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteEvent_WithNonExistentId_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            eventService.delete(99999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
