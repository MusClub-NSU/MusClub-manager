package com.nsu.musclub.web;

import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventMemberResponseDto;
import com.nsu.musclub.dto.event.EventMemberUpsertDto;
import com.nsu.musclub.dto.event.EventResponseDto;
import com.nsu.musclub.dto.event.EventTreeNodeDto;
import com.nsu.musclub.dto.event.EventUpdateDto;
import com.nsu.musclub.dto.event.PosterDescriptionResponseDto;
import com.nsu.musclub.dto.event.SocialMediaPostRequestDto;
import com.nsu.musclub.dto.event.SocialMediaPostResponseDto;
import com.nsu.musclub.service.EventPosterAiService;
import com.nsu.musclub.service.EventRelationService;
import com.nsu.musclub.service.EventService;
import com.nsu.musclub.service.SocialMediaPostAiService;
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

@Tag(name = "Events", description = "Operations with musical events, AI-generated poster descriptions, and social media posts")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final EventRelationService relations;
    private final EventPosterAiService posterAiService;
    private final SocialMediaPostAiService socialMediaPostAiService;

    public EventController(EventService service,
                           EventRelationService relations,
                           EventPosterAiService posterAiService,
                           SocialMediaPostAiService socialMediaPostAiService) {
        this.service = service;
        this.relations = relations;
        this.posterAiService = posterAiService;
        this.socialMediaPostAiService = socialMediaPostAiService;
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
                    Generates a poster description text for an event based on its data (title, time, venue, current description).
                    If save=true, the generated text is saved to the event's aiDescription field.
                    """
    )
    @PostMapping("/{eventId}/poster-description/ai")
    public PosterDescriptionResponseDto generatePosterDescription(
            @Parameter(description = "Event identifier", example = "1")
            @PathVariable Long eventId,

            @Parameter(
                    description = "If true — save the result to the event's aiDescription field",
                    example = "false"
            )
            @RequestParam(defaultValue = "false") boolean save
    ) {
        String description = posterAiService.generatePosterDescription(eventId, save);
        return new PosterDescriptionResponseDto(description);
    }

    @Operation(
            summary = "Generate AI-based social media post",
            description = """
                    Generates a social media post for an event using AI.
                    Supports multiple platforms (twitter, instagram, facebook, linkedin) and tones (casual, professional, enthusiastic, informative).
                    """
    )
    @PostMapping("/{eventId}/social-media-post/ai")
    public SocialMediaPostResponseDto generateSocialMediaPost(
            @Parameter(description = "Event identifier", example = "1")
            @PathVariable Long eventId,
            
            @Parameter(description = "Target social media platform", example = "twitter")
            @RequestParam(required = false, defaultValue = "general") String platform,
            
            @Parameter(description = "Desired tone of the post", example = "casual")
            @RequestParam(required = false, defaultValue = "casual") String tone
    ) {
        return socialMediaPostAiService.generateSocialMediaPost(eventId, platform, tone);
    }

    @Operation(
            summary = "Generate AI-based social media post with request body",
            description = """
                    Generates a social media post for an event using AI with customizable options via request body.
                    """
    )
    @PostMapping("/{eventId}/social-media-post/ai/advanced")
    public SocialMediaPostResponseDto generateSocialMediaPostAdvanced(
            @Parameter(description = "Event identifier", example = "1")
            @PathVariable Long eventId,
            
            @RequestBody(required = false) SocialMediaPostRequestDto request
    ) {
        if (request == null) {
            return socialMediaPostAiService.generateSocialMediaPost(eventId);
        }
        return socialMediaPostAiService.generateSocialMediaPost(
                eventId, 
                request.getPlatform() != null ? request.getPlatform() : "general",
                request.getTone() != null ? request.getTone() : "casual"
        );
    }
}
