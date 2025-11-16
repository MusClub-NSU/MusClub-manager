package com.nsu.musclub.web;

import com.nsu.musclub.dto.event.*;
import com.nsu.musclub.service.EventRelationService;
import com.nsu.musclub.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;
    private final EventRelationService relations;

    public EventController(EventService service, EventRelationService relations) {
        this.service = service;
        this.relations = relations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto create(@RequestBody @Valid EventCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public EventResponseDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public Page<EventResponseDto> list(@ParameterObject Pageable pageable) {
        return service.list(pageable);
    }

    @PutMapping("/{id}")
    public EventResponseDto update(@PathVariable Long id, @RequestBody @Valid EventUpdateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{eventId}/members")
    public List<EventMemberResponseDto> listMembers(@PathVariable Long eventId) {
        return relations.listMembers(eventId);
    }

    @PostMapping("/{eventId}/members")
    @ResponseStatus(HttpStatus.OK)
    public EventMemberResponseDto upsertMember(@PathVariable Long eventId,
                                               @RequestBody @Valid EventMemberUpsertDto dto) {
        return relations.upsertMember(eventId, dto);
    }

    @DeleteMapping("/{eventId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long eventId, @PathVariable Long userId) {
        relations.removeMember(eventId, userId);
    }
}
