package com.nsu.musclub.web;

import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventMemberResponseDto;
import com.nsu.musclub.dto.event.EventMemberUpsertDto;
import com.nsu.musclub.dto.event.EventProgramItemCreateDto;
import com.nsu.musclub.dto.event.EventProgramItemResponseDto;
import com.nsu.musclub.dto.event.EventProgramItemUpdateDto;
import com.nsu.musclub.dto.event.EventResponseDto;
import com.nsu.musclub.dto.event.EventTimelineItemCreateDto;
import com.nsu.musclub.dto.event.EventTimelineItemResponseDto;
import com.nsu.musclub.dto.event.EventTimelineItemUpdateDto;
import com.nsu.musclub.dto.event.EventTreeNodeDto;
import com.nsu.musclub.dto.event.EventUpdateDto;
import com.nsu.musclub.dto.event.PosterDescriptionResponseDto;
import com.nsu.musclub.dto.event.SocialMediaPostRequestDto;
import com.nsu.musclub.dto.event.SocialMediaPostResponseDto;
import com.nsu.musclub.service.EventPosterAiService;
import com.nsu.musclub.service.EventProgramService;
import com.nsu.musclub.service.EventRelationService;
import com.nsu.musclub.service.EventService;
import com.nsu.musclub.service.EventTimelineService;
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
import com.nsu.musclub.service.EventNotificationService;

import java.util.List;

@Tag(name = "Events", description = "Operations with musical events, AI-generated poster descriptions, and social media posts")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final EventRelationService relations;
    private final EventPosterAiService posterAiService;
    private final SocialMediaPostAiService socialMediaPostAiService;
    private final EventNotificationService notificationService;
    private final EventTimelineService timelineService;
    private final EventProgramService programService;

    public EventController(EventService service,
                           EventRelationService relations,
                           EventPosterAiService posterAiService,
                           SocialMediaPostAiService socialMediaPostAiService,
                           EventNotificationService notificationService,
                           EventTimelineService timelineService,
                           EventProgramService programService) {
        this.service = service;
        this.relations = relations;
        this.posterAiService = posterAiService;
        this.socialMediaPostAiService = socialMediaPostAiService;
        this.notificationService = notificationService;
        this.timelineService = timelineService;
        this.programService = programService;
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
    public EventMemberResponseDto upsertMember(@PathVariable Long eventId, @RequestBody @Valid EventMemberUpsertDto dto) {
        return relations.upsertMember(eventId, dto);
    }

    @DeleteMapping("/{eventId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long eventId, @PathVariable Long userId) {
        relations.removeMember(eventId, userId);
    }

    @GetMapping("/{eventId}/timeline")
    public List<EventTimelineItemResponseDto> listTimeline(@PathVariable Long eventId) {
        return timelineService.list(eventId);
    }

    @PostMapping("/{eventId}/timeline")
    @ResponseStatus(HttpStatus.CREATED)
    public EventTimelineItemResponseDto createTimelineItem(@PathVariable Long eventId,
                                                           @RequestBody @Valid EventTimelineItemCreateDto dto) {
        return timelineService.create(eventId, dto);
    }

    @PutMapping("/{eventId}/timeline/{itemId}")
    public EventTimelineItemResponseDto updateTimelineItem(@PathVariable Long eventId,
                                                           @PathVariable Long itemId,
                                                           @RequestBody @Valid EventTimelineItemUpdateDto dto) {
        return timelineService.update(eventId, itemId, dto);
    }

    @DeleteMapping("/{eventId}/timeline/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimelineItem(@PathVariable Long eventId, @PathVariable Long itemId) {
        timelineService.delete(eventId, itemId);
    }

    @PutMapping("/{eventId}/timeline/reorder")
    public List<EventTimelineItemResponseDto> reorderTimeline(@PathVariable Long eventId,
                                                              @RequestBody List<Long> itemIds) {
        return timelineService.reorder(eventId, itemIds);
    }

    @GetMapping("/{eventId}/program")
    public List<EventProgramItemResponseDto> listProgram(@PathVariable Long eventId) {
        return programService.list(eventId);
    }

    @PostMapping("/{eventId}/program")
    @ResponseStatus(HttpStatus.CREATED)
    public EventProgramItemResponseDto createProgramItem(@PathVariable Long eventId,
                                                         @RequestBody @Valid EventProgramItemCreateDto dto) {
        return programService.create(eventId, dto);
    }

    @PutMapping("/{eventId}/program/{itemId}")
    public EventProgramItemResponseDto updateProgramItem(@PathVariable Long eventId,
                                                         @PathVariable Long itemId,
                                                         @RequestBody @Valid EventProgramItemUpdateDto dto) {
        return programService.update(eventId, itemId, dto);
    }

    @DeleteMapping("/{eventId}/program/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProgramItem(@PathVariable Long eventId, @PathVariable Long itemId) {
        programService.delete(eventId, itemId);
    }

    @PutMapping("/{eventId}/program/reorder")
    public List<EventProgramItemResponseDto> reorderProgram(@PathVariable Long eventId,
                                                            @RequestBody List<Long> itemIds) {
        return programService.reorder(eventId, itemIds);
    }

    @PostMapping("/{parentId}/subevents")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto createSubEvent(@PathVariable Long parentId, @RequestBody @Valid EventCreateDto dto) {
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
    public EventTreeNodeDto tree(@PathVariable Long eventId, @RequestParam(defaultValue = "3") int depth) {
        return relations.getTree(eventId, depth);
    }

    @Operation(summary = "Generate AI-based poster description", description = """
            Generates a poster description text for an event based on its data (title, time, venue, current description).
            If save=true, the generated text is saved to the event's aiDescription field.
            """)
    @PostMapping("/{eventId}/poster-description/ai")
    public PosterDescriptionResponseDto generatePosterDescription(@Parameter(description = "Event identifier", example = "1") @PathVariable Long eventId,

                                                                  @Parameter(description = "If true — save the result to the event's aiDescription field", example = "false") @RequestParam(defaultValue = "false") boolean save) {
        String description = posterAiService.generatePosterDescription(eventId, save);
        return new PosterDescriptionResponseDto(description);
    }

    @Operation(summary = "Generate AI-based social media post", description = """
            Generates a social media post for an event using AI.
            Supports multiple platforms (twitter, instagram, facebook, linkedin) and tones (casual, professional, enthusiastic, informative).
            """)
    @PostMapping("/{eventId}/social-media-post/ai")
    public SocialMediaPostResponseDto generateSocialMediaPost(@Parameter(description = "Event identifier", example = "1") @PathVariable Long eventId,

                                                              @Parameter(description = "Target social media platform", example = "twitter") @RequestParam(required = false, defaultValue = "general") String platform,

                                                              @Parameter(description = "Desired tone of the post", example = "casual") @RequestParam(required = false, defaultValue = "casual") String tone) {
        return socialMediaPostAiService.generateSocialMediaPost(eventId, platform, tone);
    }

    @Operation(summary = "Generate AI-based social media post with request body", description = """
            Generates a social media post for an event using AI with customizable options via request body.
            """)
    @PostMapping("/{eventId}/social-media-post/ai/advanced")
    public SocialMediaPostResponseDto generateSocialMediaPostAdvanced(@Parameter(description = "Event identifier", example = "1") @PathVariable Long eventId,

                                                                      @RequestBody(required = false) SocialMediaPostRequestDto request) {
        if (request == null) {
            return socialMediaPostAiService.generateSocialMediaPost(eventId);
        }
        return socialMediaPostAiService.generateSocialMediaPost(eventId, request.getPlatform() != null ? request.getPlatform() : "general", request.getTone() != null ? request.getTone() : "casual");
    }

    @Operation(summary = "Schedule push notifications for event participants",
            description = "Schedules reminder notifications (24h, 2h, 15min before) for all event participants")
    @PostMapping("/{eventId}/notifications/schedule")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public java.util.Map<String, Object> scheduleNotifications(@PathVariable Long eventId) {
        int count = notificationService.scheduleNotificationsForEvent(eventId);
        return java.util.Map.of(
                "eventId", eventId,
                "scheduledCount", count,
                "message", "Notifications scheduled successfully"
        );
    }

    @Operation(summary = "Notify participants about event update")
    @PostMapping("/{eventId}/notifications/update")
    @ResponseStatus(HttpStatus.OK)
    public java.util.Map<String, Object> notifyEventUpdated(@PathVariable Long eventId) {
        int count = notificationService.notifyEventUpdated(eventId);
        return java.util.Map.of("sentCount", count);
    }

    @Operation(summary = "Notify participants about event cancellation")
    @PostMapping("/{eventId}/notifications/cancel")
    @ResponseStatus(HttpStatus.OK)
    public java.util.Map<String, Object> notifyEventCancelled(@PathVariable Long eventId) {
        int count = notificationService.notifyEventCancelled(eventId);
        return java.util.Map.of("sentCount", count);
    }
}
