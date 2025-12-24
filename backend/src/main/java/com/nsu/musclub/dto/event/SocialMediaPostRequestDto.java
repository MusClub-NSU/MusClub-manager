package com.nsu.musclub.dto.event;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for social media post generation request.
 */
public class SocialMediaPostRequestDto {

    private String platform = "general";
    private String tone = "casual";

    public SocialMediaPostRequestDto() {
    }

    public SocialMediaPostRequestDto(String platform, String tone) {
        this.platform = platform;
        this.tone = tone;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }
}

