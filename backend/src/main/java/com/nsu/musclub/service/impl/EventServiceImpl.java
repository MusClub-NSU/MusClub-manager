package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.dto.event.*;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.mapper.EventMapper;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository events;

    public EventServiceImpl(EventRepository events) {
        this.events = events;
    }

    @Override
    public EventResponseDto create(EventCreateDto dto) {
        validateEventTimes(dto.getStartTime(), dto.getEndTime());
        return EventMapper.toDto(events.save(EventMapper.toEntity(dto)));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDto get(Long id) {
        return events.findById(id).map(EventMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDto> list(Pageable pageable) {
        return events.findAll(pageable).map(EventMapper::toDto);
    }

    @Override
    public EventResponseDto update(Long id, EventUpdateDto dto) {
        var event = events.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", id));
        validateEventTimes(dto.getStartTime(), dto.getEndTime());
        EventMapper.update(dto, event);
        return EventMapper.toDto(events.save(event));
    }

    @Override
    public void delete(Long id) {
        if (!events.existsById(id)) {
            throw new ResourceNotFoundException("Мероприятие", id);
        }
        events.deleteById(id);
    }

    private void validateEventTimes(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime != null && startTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Время начала должно быть в будущем");
        }
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new BadRequestException("Время окончания должно быть позже времени начала");
        }
    }
}
