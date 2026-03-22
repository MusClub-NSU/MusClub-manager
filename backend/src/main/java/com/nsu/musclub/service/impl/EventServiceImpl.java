package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.dto.event.*;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.mapper.EventMapper;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventService;
import com.nsu.musclub.service.SearchIndexingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository events;
    private final SearchIndexingService searchIndexingService;

    public EventServiceImpl(EventRepository events,
                            SearchIndexingService searchIndexingService) {
        this.events = events;
        this.searchIndexingService = searchIndexingService;
    }

    @Override
    public EventResponseDto create(EventCreateDto dto) {
        validateEventTimes(dto.getStartTime(), dto.getEndTime());
        Event created = events.save(EventMapper.toEntity(dto));
        searchIndexingService.indexEvent(created);
        return EventMapper.toDto(created);
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
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(
                            Sort.Order.desc("startTime").nullsLast(),
                            Sort.Order.desc("id")
                    )
            );
        }
        return events.findAll(effectivePageable).map(EventMapper::toDto);
    }

    @Override
    public EventResponseDto update(Long id, EventUpdateDto dto) {
        var event = events.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Мероприятие", id));
        validateEventTimes(dto.getStartTime(), dto.getEndTime());
        EventMapper.update(dto, event);
        Event updated = events.save(event);
        searchIndexingService.indexEvent(updated);
        return EventMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        if (!events.existsById(id)) {
            throw new ResourceNotFoundException("Мероприятие", id);
        }
        events.deleteById(id);
        searchIndexingService.removeEvent(id);
    }

    private void validateEventTimes(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new BadRequestException("Время окончания должно быть позже времени начала");
        }
    }
}
