package com.nsu.musclub.service;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.ai.AiTextClient;
import com.nsu.musclub.domain.Event;
import com.nsu.musclub.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@org.springframework.transaction.annotation.Transactional
class EventPosterAiServiceTest extends AbstractIntegrationTest {

    @Autowired
    private EventPosterAiService posterAiService;

    @Autowired
    private EventRepository eventRepository;

    @MockBean
    private AiTextClient aiTextClient;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setTitle("Jazz Night");
        testEvent.setDescription("A night of smooth jazz");
        testEvent.setStartTime(OffsetDateTime.now().plusDays(1));
        testEvent.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(3));
        testEvent.setVenue("Blue Note Club");
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void generatePosterDescription_ShouldReturnDescription() {
        String expectedDescription = "Join us for an amazing Jazz Night at Blue Note Club!";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedDescription);

        String result = posterAiService.generatePosterDescription(testEvent.getId(), false);

        assertNotNull(result);
        assertEquals(expectedDescription, result);
        verify(aiTextClient, times(1)).generateText(anyString(), anyString());
    }

    @Test
    void generatePosterDescription_WithSaveTrue_ShouldSaveToEvent() {
        String expectedDescription = "Amazing event description";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedDescription);

        String result = posterAiService.generatePosterDescription(testEvent.getId(), true);

        assertEquals(expectedDescription, result);
        
        Event savedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertEquals(expectedDescription, savedEvent.getAiDescription());
    }

    @Test
    void generatePosterDescription_WithSaveFalse_ShouldNotSaveToEvent() {
        String expectedDescription = "Event description";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedDescription);

        posterAiService.generatePosterDescription(testEvent.getId(), false);

        Event savedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertNull(savedEvent.getAiDescription());
    }

    @Test
    void generatePosterDescription_WithNonExistentEvent_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            posterAiService.generatePosterDescription(99999L, false);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(aiTextClient, never()).generateText(anyString(), anyString());
    }

    @Test
    void generatePosterDescription_WithPaymentError_ShouldThrow502() {
        HttpClientErrorException paymentException = new HttpClientErrorException(
                org.springframework.http.HttpStatus.valueOf(402),
                "Insufficient Balance"
        );
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(paymentException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            posterAiService.generatePosterDescription(testEvent.getId(), false);
        });

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Insufficient Balance"));
    }

    @Test
    void generatePosterDescription_WithClientError_ShouldThrow503() {
        HttpClientErrorException clientException = new HttpClientErrorException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Bad Request"
        );
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(clientException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            posterAiService.generatePosterDescription(testEvent.getId(), false);
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void generatePosterDescription_WithServerError_ShouldThrow503() {
        HttpServerErrorException serverException = new HttpServerErrorException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error"
        );
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(serverException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            posterAiService.generatePosterDescription(testEvent.getId(), false);
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void generatePosterDescription_WithUnexpectedError_ShouldThrow503() {
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(new RuntimeException("Unexpected error"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            posterAiService.generatePosterDescription(testEvent.getId(), false);
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void generatePosterDescription_WithAllEventFields_ShouldIncludeAllInPrompt() {
        String expectedDescription = "Complete event description";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedDescription);

        posterAiService.generatePosterDescription(testEvent.getId(), false);

        verify(aiTextClient, times(1)).generateText(anyString(), argThat(prompt -> 
            prompt.contains("Jazz Night") &&
            prompt.contains("Blue Note Club") &&
            prompt.contains("smooth jazz")
        ));
    }

    @Test
    void generatePosterDescription_WithMinimalEventData_ShouldStillWork() {
        Event minimalEvent = new Event();
        minimalEvent.setTitle("Minimal Event");
        minimalEvent.setStartTime(OffsetDateTime.now().plusDays(1));
        minimalEvent = eventRepository.save(minimalEvent);

        String expectedDescription = "Minimal description";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedDescription);

        String result = posterAiService.generatePosterDescription(minimalEvent.getId(), false);

        assertNotNull(result);
        assertEquals(expectedDescription, result);
    }
}

