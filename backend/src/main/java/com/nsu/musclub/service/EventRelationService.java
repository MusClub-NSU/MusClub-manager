package com.nsu.musclub.service;

import com.nsu.musclub.dto.event.*;

import java.util.List;

public interface EventRelationService {
    List<EventMemberResponseDto> listMembers(Long eventId);
    EventMemberResponseDto upsertMember(Long eventId, EventMemberUpsertDto dto);
    void removeMember(Long eventId, Long userId);

    EventResponseDto createSubEvent(Long parentId, EventCreateDto dto);
    void attachChild(Long parentId, Long childId);
    void detachChild(Long parentId, Long childId);
    EventTreeNodeDto getTree(Long eventId, int depth);
}

