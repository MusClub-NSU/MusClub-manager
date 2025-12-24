package com.nsu.musclub.dto.event;

/**
 * DTO for social media post generation response.
 */
public class SocialMediaPostResponseDto {

    private String content;
    private String platform;
    private String tone;

    public SocialMediaPostResponseDto() {
    }

    public SocialMediaPostResponseDto(String content, String platform, String tone) {
        this.content = content;
        this.platform = platform;
        this.tone = tone;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

