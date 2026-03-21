package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.EventTimelineItem;
import com.nsu.musclub.dto.event.EventTimelineItemCreateDto;
import com.nsu.musclub.dto.event.EventTimelineItemResponseDto;
import com.nsu.musclub.dto.event.EventTimelineItemUpdateDto;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.repository.EventTimelineItemRepository;
import com.nsu.musclub.service.EventTimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class EventTimelineServiceImpl implements EventTimelineService {
    private final EventRepository events;
    private final EventTimelineItemRepository timelineItems;

    public EventTimelineServiceImpl(EventRepository events, EventTimelineItemRepository timelineItems) {
        this.events = events;
        this.timelineItems = timelineItems;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventTimelineItemResponseDto> list(Long eventId) {
        ensureEvent(eventId);
        return timelineItems.findByEvent_IdOrderByPositionAsc(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public EventTimelineItemResponseDto create(Long eventId, EventTimelineItemCreateDto dto) {
        var event = ensureEvent(eventId);
        EventTimelineItem item = new EventTimelineItem();
        item.setEvent(event);
        item.setPlannedTime(dto.getPlannedTime());
        item.setDescription(dto.getDescription().trim());
        item.setPosition(timelineItems.findByEvent_IdOrderByPositionAsc(eventId).size() + 1);
        return toDto(timelineItems.save(item));
    }

    @Override
    public EventTimelineItemResponseDto update(Long eventId, Long itemId, EventTimelineItemUpdateDto dto) {
        ensureEvent(eventId);
        EventTimelineItem item = timelineItems.findByIdAndEvent_Id(itemId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент таймплана", itemId));
        item.setPlannedTime(dto.getPlannedTime());
        item.setDescription(dto.getDescription().trim());
        return toDto(timelineItems.save(item));
    }

    @Override
    public void delete(Long eventId, Long itemId) {
        ensureEvent(eventId);
        EventTimelineItem item = timelineItems.findByIdAndEvent_Id(itemId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент таймплана", itemId));
        int removedPos = item.getPosition();
        timelineItems.delete(item);

        List<EventTimelineItem> siblings = timelineItems.findByEvent_IdOrderByPositionAsc(eventId);
        for (EventTimelineItem sibling : siblings) {
            if (sibling.getPosition() > removedPos) {
                sibling.setPosition(sibling.getPosition() - 1);
            }
        }
        timelineItems.saveAll(siblings);
    }

    @Override
    public List<EventTimelineItemResponseDto> reorder(Long eventId, List<Long> itemIds) {
        ensureEvent(eventId);
        if (itemIds == null) {
            throw new BadRequestException("Список reorder не должен быть null", "INVALID_REORDER");
        }
        List<EventTimelineItem> existing = timelineItems.findByEvent_IdOrderByPositionAsc(eventId);
        if (existing.size() != itemIds.size()) {
            throw new BadRequestException("Список reorder должен содержать все элементы таймплана", "INVALID_REORDER");
        }

        Set<Long> expected = existing.stream().map(EventTimelineItem::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> actual = new HashSet<>(itemIds);
        if (!expected.equals(actual)) {
            throw new BadRequestException("Список reorder содержит некорректные id", "INVALID_REORDER");
        }

        java.util.Map<Long, EventTimelineItem> byId = existing.stream()
                .collect(java.util.stream.Collectors.toMap(EventTimelineItem::getId, i -> i));
        for (EventTimelineItem item : existing) {
            item.setPosition(item.getPosition() + 10000);
        }
        timelineItems.saveAll(existing);
        for (int i = 0; i < itemIds.size(); i++) {
            byId.get(itemIds.get(i)).setPosition(i + 1);
        }
        timelineItems.saveAll(existing);
        return timelineItems.findByEvent_IdOrderByPositionAsc(eventId).stream().map(this::toDto).toList();
    }

    private com.nsu.musclub.domain.Event ensureEvent(Long id) {
        return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Мероприятие", id));
    }

    private EventTimelineItemResponseDto toDto(EventTimelineItem item) {
        EventTimelineItemResponseDto dto = new EventTimelineItemResponseDto();
        dto.setId(item.getId());
        dto.setPlannedTime(item.getPlannedTime());
        dto.setDescription(item.getDescription());
        dto.setPosition(item.getPosition());
        return dto;
    }
}
