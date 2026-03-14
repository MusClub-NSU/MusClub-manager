package com.nsu.musclub.service;

import com.nsu.musclub.dto.event.EventProgramItemCreateDto;
import com.nsu.musclub.dto.event.EventProgramItemResponseDto;
import com.nsu.musclub.dto.event.EventProgramItemUpdateDto;

import java.util.List;

public interface EventProgramService {
    List<EventProgramItemResponseDto> list(Long eventId);

    EventProgramItemResponseDto create(Long eventId, EventProgramItemCreateDto dto);

    EventProgramItemResponseDto update(Long eventId, Long itemId, EventProgramItemUpdateDto dto);

    void delete(Long eventId, Long itemId);

    List<EventProgramItemResponseDto> reorder(Long eventId, List<Long> itemIds);
}
