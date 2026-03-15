package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.EventProgramItem;
import com.nsu.musclub.dto.event.EventProgramItemCreateDto;
import com.nsu.musclub.dto.event.EventProgramItemResponseDto;
import com.nsu.musclub.dto.event.EventProgramItemUpdateDto;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.repository.EventProgramItemRepository;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.service.EventProgramService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class EventProgramServiceImpl implements EventProgramService {
    private final EventRepository events;
    private final EventProgramItemRepository programItems;

    public EventProgramServiceImpl(EventRepository events, EventProgramItemRepository programItems) {
        this.events = events;
        this.programItems = programItems;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventProgramItemResponseDto> list(Long eventId) {
        ensureEvent(eventId);
        return programItems.findByEvent_IdOrderByPositionAsc(eventId).stream().map(this::toDto).toList();
    }

    @Override
    public EventProgramItemResponseDto create(Long eventId, EventProgramItemCreateDto dto) {
        var event = ensureEvent(eventId);
        EventProgramItem item = new EventProgramItem();
        item.setEvent(event);
        item.setTitle(dto.getTitle().trim());
        item.setArtist(trimToNull(dto.getArtist()));
        item.setPlannedTime(dto.getPlannedTime());
        item.setDurationText(trimToNull(dto.getDurationText()));
        item.setNotes(trimToNull(dto.getNotes()));
        item.setPosition(programItems.findByEvent_IdOrderByPositionAsc(eventId).size() + 1);
        return toDto(programItems.save(item));
    }

    @Override
    public EventProgramItemResponseDto update(Long eventId, Long itemId, EventProgramItemUpdateDto dto) {
        ensureEvent(eventId);
        EventProgramItem item = programItems.findByIdAndEvent_Id(itemId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент программы", itemId));
        item.setTitle(dto.getTitle().trim());
        item.setArtist(trimToNull(dto.getArtist()));
        item.setPlannedTime(dto.getPlannedTime());
        item.setDurationText(trimToNull(dto.getDurationText()));
        item.setNotes(trimToNull(dto.getNotes()));
        return toDto(programItems.save(item));
    }

    @Override
    public void delete(Long eventId, Long itemId) {
        ensureEvent(eventId);
        EventProgramItem item = programItems.findByIdAndEvent_Id(itemId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент программы", itemId));
        int removedPos = item.getPosition();
        programItems.delete(item);

        List<EventProgramItem> siblings = programItems.findByEvent_IdOrderByPositionAsc(eventId);
        for (EventProgramItem sibling : siblings) {
            if (sibling.getPosition() > removedPos) {
                sibling.setPosition(sibling.getPosition() - 1);
            }
        }
        programItems.saveAll(siblings);
    }

    @Override
    public List<EventProgramItemResponseDto> reorder(Long eventId, List<Long> itemIds) {
        ensureEvent(eventId);
        if (itemIds == null) {
            throw new BadRequestException("Список reorder не должен быть null", "INVALID_REORDER");
        }
        List<EventProgramItem> existing = programItems.findByEvent_IdOrderByPositionAsc(eventId);
        if (existing.size() != itemIds.size()) {
            throw new BadRequestException("Список reorder должен содержать все элементы программы", "INVALID_REORDER");
        }

        Set<Long> expected = existing.stream().map(EventProgramItem::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> actual = new HashSet<>(itemIds);
        if (!expected.equals(actual)) {
            throw new BadRequestException("Список reorder содержит некорректные id", "INVALID_REORDER");
        }

        java.util.Map<Long, EventProgramItem> byId = existing.stream()
                .collect(java.util.stream.Collectors.toMap(EventProgramItem::getId, i -> i));
        for (EventProgramItem item : existing) {
            item.setPosition(item.getPosition() + 10000);
        }
        programItems.saveAll(existing);
        for (int i = 0; i < itemIds.size(); i++) {
            byId.get(itemIds.get(i)).setPosition(i + 1);
        }
        programItems.saveAll(existing);
        return programItems.findByEvent_IdOrderByPositionAsc(eventId).stream().map(this::toDto).toList();
    }

    private com.nsu.musclub.domain.Event ensureEvent(Long id) {
        return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Мероприятие", id));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private EventProgramItemResponseDto toDto(EventProgramItem item) {
        EventProgramItemResponseDto dto = new EventProgramItemResponseDto();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setArtist(item.getArtist());
        dto.setPlannedTime(item.getPlannedTime());
        dto.setDurationText(item.getDurationText());
        dto.setNotes(item.getNotes());
        dto.setPosition(item.getPosition());
        return dto;
    }
}
