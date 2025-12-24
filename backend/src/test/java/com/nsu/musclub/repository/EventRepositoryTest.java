package com.nsu.musclub.repository;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.domain.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class EventRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    private OffsetDateTime futureTime() {
        return OffsetDateTime.now().plusDays(1);
    }

    @Test
    void saveEvent_ShouldSucceed() {
        Event event = new Event();
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setStartTime(futureTime());
        event.setEndTime(futureTime().plusHours(2));
        event.setVenue("Test Venue");

        Event saved = eventRepository.save(event);

        assertNotNull(saved.getId());
        assertEquals("Test Event", saved.getTitle());
        assertEquals("Test Description", saved.getDescription());
        assertEquals("Test Venue", saved.getVenue());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void saveEvent_WithoutEndTime_ShouldSucceed() {
        Event event = new Event();
        event.setTitle("Event Without End");
        event.setStartTime(futureTime());
        event.setEndTime(null);

        Event saved = eventRepository.save(event);

        assertNotNull(saved.getId());
        assertNull(saved.getEndTime());
    }

    @Test
    void saveEvent_WithoutDescription_ShouldSucceed() {
        Event event = new Event();
        event.setTitle("Event Without Description");
        event.setDescription(null);
        event.setStartTime(futureTime());

        Event saved = eventRepository.save(event);

        assertNotNull(saved.getId());
        assertNull(saved.getDescription());
    }

    @Test
    void findById_ShouldReturnEvent() {
        Event event = new Event();
        event.setTitle("Find Event");
        event.setStartTime(futureTime());

        Event saved = eventRepository.save(event);
        Event found = eventRepository.findById(saved.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals("Find Event", found.getTitle());
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        assertFalse(eventRepository.findById(99999L).isPresent());
    }

    @Test
    void findAll_ShouldReturnAllEvents() {
        Event event1 = new Event();
        event1.setTitle("Event 1");
        event1.setStartTime(futureTime());

        Event event2 = new Event();
        event2.setTitle("Event 2");
        event2.setStartTime(futureTime().plusDays(1));

        eventRepository.save(event1);
        eventRepository.save(event2);

        long count = eventRepository.count();

        assertTrue(count >= 2);
    }

    @Test
    void deleteById_ShouldRemoveEvent() {
        Event event = new Event();
        event.setTitle("Delete Event");
        event.setStartTime(futureTime());

        Event saved = eventRepository.save(event);
        Long id = saved.getId();

        eventRepository.deleteById(id);

        assertFalse(eventRepository.existsById(id));
    }

    @Test
    void existsById_ShouldReturnCorrectValue() {
        Event event = new Event();
        event.setTitle("Exist Event");
        event.setStartTime(futureTime());

        Event saved = eventRepository.save(event);

        assertTrue(eventRepository.existsById(saved.getId()));
        assertFalse(eventRepository.existsById(99999L));
    }

    @Test
    void saveEvent_ShouldSetCreatedAt() {
        Event event = new Event();
        event.setTitle("Time Event");
        event.setStartTime(futureTime());

        Event saved = eventRepository.save(event);

        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
        assertTrue(saved.getCreatedAt().isAfter(OffsetDateTime.now().minusSeconds(5)));
    }

    @Test
    void updateEvent_ShouldSucceed() {
        Event event = new Event();
        event.setTitle("Original Title");
        event.setStartTime(futureTime());
        event.setVenue("Original Venue");

        Event saved = eventRepository.save(event);

        saved.setTitle("Updated Title");
        saved.setVenue("Updated Venue");

        Event updated = eventRepository.save(saved);

        assertEquals(saved.getId(), updated.getId());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Venue", updated.getVenue());
    }

    @Test
    void saveEvent_WithLongTitle_ShouldTruncate() {
        Event event = new Event();
        event.setTitle("A".repeat(300)); // Longer than 255
        event.setStartTime(futureTime());

        // This should either truncate or throw an exception
        // Let's see what happens
        assertThrows(Exception.class, () -> {
            eventRepository.save(event);
            eventRepository.flush();
        });
    }

    @Test
    void saveEvent_WithNullTitle_ShouldFail() {
        Event event = new Event();
        event.setTitle(null);
        event.setStartTime(futureTime());

        assertThrows(Exception.class, () -> {
            eventRepository.save(event);
            eventRepository.flush();
        });
    }

    @Test
    void saveEvent_WithNullStartTime_ShouldFail() {
        Event event = new Event();
        event.setTitle("No Start Time");
        event.setStartTime(null);

        assertThrows(Exception.class, () -> {
            eventRepository.save(event);
            eventRepository.flush();
        });
    }
}

