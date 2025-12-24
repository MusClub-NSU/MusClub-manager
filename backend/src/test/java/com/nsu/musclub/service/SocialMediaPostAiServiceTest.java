package com.nsu.musclub.service;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.ai.AiTextClient;
import com.nsu.musclub.domain.Event;
import com.nsu.musclub.dto.event.SocialMediaPostResponseDto;
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
class SocialMediaPostAiServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SocialMediaPostAiService socialMediaPostAiService;

    @Autowired
    private EventRepository eventRepository;

    @MockBean
    private AiTextClient aiTextClient;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setTitle("Rock Concert 2024");
        testEvent.setDescription("An amazing rock concert featuring top artists");
        testEvent.setStartTime(OffsetDateTime.now().plusDays(7));
        testEvent.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(4));
        testEvent.setVenue("Stadium Arena");
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void generateSocialMediaPost_WithDefaultParams_ShouldReturnPost() {
        String expectedPost = "Join us for Rock Concert 2024! #RockConcert #Music";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        SocialMediaPostResponseDto result = socialMediaPostAiService.generateSocialMediaPost(testEvent.getId());

        assertNotNull(result);
        assertEquals(expectedPost, result.getContent());
        assertEquals("general", result.getPlatform());
        assertEquals("casual", result.getTone());
        verify(aiTextClient, times(1)).generateText(anyString(), anyString());
    }

    @Test
    void generateSocialMediaPost_WithTwitterPlatform_ShouldReturnTwitterPost() {
        String expectedPost = "Rock Concert 2024 is coming! 🎸 #RockConcert";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        SocialMediaPostResponseDto result = socialMediaPostAiService.generateSocialMediaPost(
                testEvent.getId(), "twitter", "casual");

        assertNotNull(result);
        assertEquals(expectedPost, result.getContent());
        assertEquals("twitter", result.getPlatform());
        assertEquals("casual", result.getTone());
        verify(aiTextClient, times(1)).generateText(
                argThat(prompt -> prompt.contains("Twitter/X")), anyString());
    }

    @Test
    void generateSocialMediaPost_WithInstagramPlatform_ShouldReturnInstagramPost() {
        String expectedPost = "Get ready for an amazing night! 🎵✨\n\nRock Concert 2024\n#RockConcert #Music";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        SocialMediaPostResponseDto result = socialMediaPostAiService.generateSocialMediaPost(
                testEvent.getId(), "instagram", "enthusiastic");

        assertNotNull(result);
        assertEquals(expectedPost, result.getContent());
        assertEquals("instagram", result.getPlatform());
        assertEquals("enthusiastic", result.getTone());
        verify(aiTextClient, times(1)).generateText(
                argThat(prompt -> prompt.contains("Instagram")), anyString());
    }

    @Test
    void generateSocialMediaPost_WithFacebookPlatform_ShouldReturnFacebookPost() {
        String expectedPost = "Join us for Rock Concert 2024 at Stadium Arena!";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        SocialMediaPostResponseDto result = socialMediaPostAiService.generateSocialMediaPost(
                testEvent.getId(), "facebook", "professional");

        assertNotNull(result);
        assertEquals(expectedPost, result.getContent());
        assertEquals("facebook", result.getPlatform());
        assertEquals("professional", result.getTone());
    }

    @Test
    void generateSocialMediaPost_WithLinkedInPlatform_ShouldReturnLinkedInPost() {
        String expectedPost = "Professional networking event: Rock Concert 2024";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        SocialMediaPostResponseDto result = socialMediaPostAiService.generateSocialMediaPost(
                testEvent.getId(), "linkedin", "professional");

        assertNotNull(result);
        assertEquals(expectedPost, result.getContent());
        assertEquals("linkedin", result.getPlatform());
        assertEquals("professional", result.getTone());
        verify(aiTextClient, times(1)).generateText(
                argThat(prompt -> prompt.contains("LinkedIn")), anyString());
    }

    @Test
    void generateSocialMediaPost_WithDifferentTones_ShouldUseCorrectTone() {
        String expectedPost = "Test post";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        // Test enthusiastic tone
        SocialMediaPostResponseDto result1 = socialMediaPostAiService.generateSocialMediaPost(
                testEvent.getId(), "general", "enthusiastic");
        assertEquals("enthusiastic", result1.getTone());
        verify(aiTextClient, times(1)).generateText(
                argThat(prompt -> prompt.contains("Tone: enthusiastic")), anyString());

        // Test informative tone
        SocialMediaPostResponseDto result2 = socialMediaPostAiService.generateSocialMediaPost(
                testEvent.getId(), "general", "informative");
        assertEquals("informative", result2.getTone());
        verify(aiTextClient, times(2)).generateText(anyString(), anyString());
    }

    @Test
    void generateSocialMediaPost_WithNonExistentEvent_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            socialMediaPostAiService.generateSocialMediaPost(99999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(aiTextClient, never()).generateText(anyString(), anyString());
    }

    @Test
    void generateSocialMediaPost_WithPaymentError_ShouldThrow502() {
        HttpClientErrorException paymentException = new HttpClientErrorException(
                org.springframework.http.HttpStatus.valueOf(402),
                "Insufficient Balance"
        );
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(paymentException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            socialMediaPostAiService.generateSocialMediaPost(testEvent.getId());
        });

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Insufficient Balance"));
    }

    @Test
    void generateSocialMediaPost_WithClientError_ShouldThrow503() {
        HttpClientErrorException clientException = new HttpClientErrorException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Bad Request"
        );
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(clientException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            socialMediaPostAiService.generateSocialMediaPost(testEvent.getId());
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void generateSocialMediaPost_WithServerError_ShouldThrow503() {
        HttpServerErrorException serverException = new HttpServerErrorException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error"
        );
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(serverException);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            socialMediaPostAiService.generateSocialMediaPost(testEvent.getId());
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void generateSocialMediaPost_WithUnexpectedError_ShouldThrow503() {
        when(aiTextClient.generateText(anyString(), anyString())).thenThrow(new RuntimeException("Unexpected error"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            socialMediaPostAiService.generateSocialMediaPost(testEvent.getId());
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void generateSocialMediaPost_ShouldIncludeEventDataInPrompt() {
        String expectedPost = "Test post";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        socialMediaPostAiService.generateSocialMediaPost(testEvent.getId(), "general", "casual");

        verify(aiTextClient, times(1)).generateText(anyString(), argThat(prompt -> 
            prompt.contains("Rock Concert 2024") &&
            prompt.contains("Stadium Arena") &&
            prompt.contains("rock concert")
        ));
    }

    @Test
    void generateSocialMediaPost_WithAiDescription_ShouldIncludeInPrompt() {
        testEvent.setAiDescription("AI-generated description");
        testEvent = eventRepository.save(testEvent);

        String expectedPost = "Test post";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        socialMediaPostAiService.generateSocialMediaPost(testEvent.getId());

        verify(aiTextClient, times(1)).generateText(anyString(), argThat(prompt -> 
            prompt.contains("AI-Generated Poster Description") &&
            prompt.contains("AI-generated description")
        ));
    }

    @Test
    void generateSocialMediaPost_WithMinimalEventData_ShouldStillWork() {
        Event minimalEvent = new Event();
        minimalEvent.setTitle("Minimal Event");
        minimalEvent.setStartTime(OffsetDateTime.now().plusDays(1));
        minimalEvent = eventRepository.save(minimalEvent);

        String expectedPost = "Minimal post";
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn(expectedPost);

        SocialMediaPostResponseDto result = socialMediaPostAiService.generateSocialMediaPost(minimalEvent.getId());

        assertNotNull(result);
        assertEquals(expectedPost, result.getContent());
    }
}

