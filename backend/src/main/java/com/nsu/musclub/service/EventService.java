package com.nsu.musclub.service;

import com.nsu.musclub.dto.event.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {
    EventResponseDto create(EventCreateDto dto);

    EventResponseDto get(Long id);

    Page<EventResponseDto> list(Pageable pageable);

    EventResponseDto update(Long id, EventUpdateDto dto);

    void delete(Long id);
}
