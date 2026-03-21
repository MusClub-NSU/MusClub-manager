package com.nsu.musclub.service;

import com.nsu.musclub.dto.event.EventTimelineItemCreateDto;
import com.nsu.musclub.dto.event.EventTimelineItemResponseDto;
import com.nsu.musclub.dto.event.EventTimelineItemUpdateDto;

import java.util.List;

public interface EventTimelineService {
    List<EventTimelineItemResponseDto> list(Long eventId);

    EventTimelineItemResponseDto create(Long eventId, EventTimelineItemCreateDto dto);

    EventTimelineItemResponseDto update(Long eventId, Long itemId, EventTimelineItemUpdateDto dto);

    void delete(Long eventId, Long itemId);

    List<EventTimelineItemResponseDto> reorder(Long eventId, List<Long> itemIds);
}
