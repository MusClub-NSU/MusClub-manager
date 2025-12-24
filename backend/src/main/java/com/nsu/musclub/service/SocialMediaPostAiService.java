package com.nsu.musclub.service;

import com.nsu.musclub.dto.event.SocialMediaPostResponseDto;

/**
 * Service interface for generating social media posts for events using AI.
 */
public interface SocialMediaPostAiService {
    
    /**
     * Generates a social media post for the specified event.
     * 
     * @param eventId the ID of the event
     * @param platform the target social media platform (e.g., "twitter", "instagram", "facebook")
     * @param tone the desired tone of the post (e.g., "casual", "professional", "enthusiastic")
     * @return SocialMediaPostResponseDto containing the generated post content
     */
    SocialMediaPostResponseDto generateSocialMediaPost(Long eventId, String platform, String tone);
    
    /**
     * Generates a social media post with default settings.
     * 
     * @param eventId the ID of the event
     * @return SocialMediaPostResponseDto containing the generated post content
     */
    SocialMediaPostResponseDto generateSocialMediaPost(Long eventId);
}

