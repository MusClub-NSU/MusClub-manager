package com.nsu.musclub.web;

import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventMemberResponseDto;
import com.nsu.musclub.dto.event.EventMemberUpsertDto;
import com.nsu.musclub.dto.event.EventResponseDto;
import com.nsu.musclub.dto.event.EventTreeNodeDto;
import com.nsu.musclub.dto.event.EventUpdateDto;
import com.nsu.musclub.dto.event.PosterDescriptionResponseDto;
import com.nsu.musclub.service.EventPosterAiService;
import com.nsu.musclub.service.EventRelationService;
import com.nsu.musclub.service.EventService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Events", description = "Operations with musical events and AI-generated poster descriptions")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final EventRelationService relations;
    private final EventPosterAiService posterAiService;

    public EventController(EventService service,
                           EventRelationService relations,
                           EventPosterAiService posterAiService) {
        this.service = service;
        this.relations = relations;
        this.posterAiService = posterAiService;
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

    @PostMapping("/{parentId}/subevents")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto createSubEvent(@PathVariable Long parentId,
                                           @RequestBody @Valid EventCreateDto dto) {
        return relations.createSubEvent(parentId, dto);
    }

    @PostMapping("/{parentId}/subevents/{childId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachChild(@PathVariable Long parentId, @PathVariable Long childId) {
        relations.attachChild(parentId, childId);
    }

    @DeleteMapping("/{parentId}/subevents/{childId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachChild(@PathVariable Long parentId, @PathVariable Long childId) {
        relations.detachChild(parentId, childId);
    }

    @GetMapping("/{eventId}/tree")
    public EventTreeNodeDto tree(@PathVariable Long eventId,
                                 @RequestParam(defaultValue = "3") int depth) {
        return relations.getTree(eventId, depth);
    }

    @Operation(
            summary = "Generate AI-based poster description",
            description = """
                    Генерирует текст описания афиши для события на основе его данных (название, время, место, текущее описание).
                    При save=true сгенерированный текст сохраняется в поле aiDescription события.
                    """
    )
    @PostMapping("/{eventId}/poster-description/ai")
    public PosterDescriptionResponseDto generatePosterDescription(
            @Parameter(description = "Идентификатор события", example = "1")
            @PathVariable Long eventId,

            @Parameter(
                    description = "Если true — сохранить результат в поле aiDescription события",
                    example = "false"
            )
            @RequestParam(defaultValue = "false") boolean save
    ) {
        String description = posterAiService.generatePosterDescription(eventId, save);
        return new PosterDescriptionResponseDto(description);
    }
}
