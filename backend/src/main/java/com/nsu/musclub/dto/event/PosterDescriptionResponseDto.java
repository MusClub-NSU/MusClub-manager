package com.nsu.musclub.dto.event;

public class PosterDescriptionResponseDto {

    private String description;

    public PosterDescriptionResponseDto() {
    }

    public PosterDescriptionResponseDto(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
