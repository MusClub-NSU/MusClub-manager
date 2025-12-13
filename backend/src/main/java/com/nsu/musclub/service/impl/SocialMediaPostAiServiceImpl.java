package com.nsu.musclub.service.impl;

import com.nsu.musclub.ai.AiTextClient;
import com.nsu.musclub.domain.Event;
import com.nsu.musclub.dto.event.SocialMediaPostResponseDto;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.SocialMediaPostAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;

/**
 * Implementation of SocialMediaPostAiService for generating social media posts using AI.
 */
@Service
@Transactional
public class SocialMediaPostAiServiceImpl implements SocialMediaPostAiService {

    private static final Logger log = LoggerFactory.getLogger(SocialMediaPostAiServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EventRepository eventRepository;
    private final AiTextClient aiTextClient;

    public SocialMediaPostAiServiceImpl(EventRepository eventRepository, AiTextClient aiTextClient) {
        this.eventRepository = eventRepository;
        this.aiTextClient = aiTextClient;
    }

    @Override
    public SocialMediaPostResponseDto generateSocialMediaPost(Long eventId) {
        return generateSocialMediaPost(eventId, "general", "casual");
    }

    @Override
    public SocialMediaPostResponseDto generateSocialMediaPost(Long eventId, String platform, String tone) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String systemPrompt = buildSystemPrompt(platform, tone);
        String userPrompt = buildUserPrompt(event, platform);

        String postContent;
        try {
            postContent = aiTextClient.generateText(systemPrompt, userPrompt);
        } catch (HttpClientErrorException e) {
            log.error("AI provider client error while generating social media post for event {}: status={}, body={}",
                    eventId, e.getStatusCode(), e.getResponseBodyAsString(), e);

            if (e.getStatusCode().value() == 402) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI provider returned payment error (Insufficient Balance). Please contact administrator."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Error calling AI provider: " + e.getStatusCode()
            );
        } catch (HttpServerErrorException e) {
            log.error("AI provider server error while generating social media post for event {}: status={}, body={}",
                    eventId, e.getStatusCode(), e.getResponseBodyAsString(), e);

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI provider temporarily unavailable, please try again later"
            );
        } catch (Exception e) {
            log.error("Unexpected error while generating AI social media post for event {}", eventId, e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to generate social media post, please try again later"
            );
        }

        return new SocialMediaPostResponseDto(postContent, platform, tone);
    }

    private String buildSystemPrompt(String platform, String tone) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a social media content creator for music events.\n");
        sb.append("Generate engaging, authentic social media posts in English.\n\n");

        // Platform-specific guidelines
        switch (platform.toLowerCase()) {
            case "twitter", "x":
                sb.append("Platform: Twitter/X\n");
                sb.append("- Maximum 280 characters (keep it concise)\n");
                sb.append("- Use hashtags strategically (2-3 relevant hashtags)\n");
                sb.append("- Include a call-to-action\n");
                break;
            case "instagram":
                sb.append("Platform: Instagram\n");
                sb.append("- Write engaging caption (150-300 words)\n");
                sb.append("- Use relevant hashtags (5-10 hashtags at the end)\n");
                sb.append("- Include emojis sparingly for visual appeal\n");
                sb.append("- Create excitement and FOMO\n");
                break;
            case "facebook":
                sb.append("Platform: Facebook\n");
                sb.append("- Write a friendly, informative post (100-200 words)\n");
                sb.append("- Include event details clearly\n");
                sb.append("- Encourage engagement (likes, shares, comments)\n");
                break;
            case "linkedin":
                sb.append("Platform: LinkedIn\n");
                sb.append("- Professional tone, but still engaging\n");
                sb.append("- Focus on networking and professional development aspects\n");
                sb.append("- 150-250 words\n");
                break;
            default:
                sb.append("Platform: General\n");
                sb.append("- Write an engaging post (100-200 words)\n");
                sb.append("- Include key event information\n");
        }

        // Tone guidelines
        sb.append("\nTone: ").append(tone).append("\n");
        switch (tone.toLowerCase()) {
            case "casual":
                sb.append("- Friendly, conversational style\n");
                sb.append("- Use contractions and everyday language\n");
                break;
            case "professional":
                sb.append("- Formal but approachable\n");
                sb.append("- Clear and structured\n");
                break;
            case "enthusiastic":
                sb.append("- Energetic and exciting\n");
                sb.append("- Use exclamation marks sparingly\n");
                sb.append("- Create urgency and excitement\n");
                break;
            case "informative":
                sb.append("- Clear and factual\n");
                sb.append("- Focus on key details\n");
                break;
        }

        sb.append("\nRequirements:\n");
        sb.append("- Only use information provided about the event\n");
        sb.append("- Do not invent facts or details\n");
        sb.append("- Make it engaging and shareable\n");
        sb.append("- Include date, time, and venue if available\n");

        return sb.toString();
    }

    private String buildUserPrompt(Event event, String platform) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a social media post for the following music event:\n\n");
        sb.append("Title: ").append(event.getTitle()).append("\n");

        if (event.getStartTime() != null) {
            sb.append("Start Time: ")
                    .append(DATE_TIME_FORMATTER.format(event.getStartTime()))
                    .append("\n");
        }

        if (event.getEndTime() != null) {
            sb.append("End Time: ")
                    .append(DATE_TIME_FORMATTER.format(event.getEndTime()))
                    .append("\n");
        }

        if (event.getVenue() != null && !event.getVenue().isBlank()) {
            sb.append("Venue: ").append(event.getVenue()).append("\n");
        }

        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append("Event Description: ").append(event.getDescription()).append("\n");
        }

        if (event.getAiDescription() != null && !event.getAiDescription().isBlank()) {
            sb.append("AI-Generated Poster Description: ").append(event.getAiDescription()).append("\n");
        }

        sb.append("\nGenerate the social media post now.");

        return sb.toString();
    }
}

