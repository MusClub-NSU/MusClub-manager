package com.nsu.musclub.web;

import com.nsu.musclub.dto.event.*;
import com.nsu.musclub.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
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
}
