package com.nsu.musclub.mapper;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.domain.EventStatus;
import com.nsu.musclub.dto.event.*;

public class EventMapper {
    public static Event toEntity(EventCreateDto dto) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setVenue(dto.getVenue());
        event.setStatus(dto.getStatus() != null ? dto.getStatus() : EventStatus.NOT_STARTED);
        return event;
    }

    public static void update(EventUpdateDto dto, Event event) {
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setVenue(dto.getVenue());
        event.setStatus(dto.getStatus() != null ? dto.getStatus() : event.getStatus());
    }

    public static EventResponseDto toDto(Event event) {
        EventResponseDto dto = new EventResponseDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setVenue(event.getVenue());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setAiDescription(event.getAiDescription());
        dto.setStatus(event.getStatus());
        return dto;
    }
}
