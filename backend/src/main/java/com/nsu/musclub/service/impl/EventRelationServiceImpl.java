package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.domain.EventMember;
import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.event.*;
import com.nsu.musclub.mapper.EventMapper;
import com.nsu.musclub.repository.EventMemberRepository;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.repository.UserRepository;
import com.nsu.musclub.service.EventRelationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EventRelationServiceImpl implements EventRelationService {
    private final EventRepository events;
    private final UserRepository users;
    private final EventMemberRepository members;

    public EventRelationServiceImpl(EventRepository events, UserRepository users, EventMemberRepository members) {
        this.events = events;
        this.users = users;
        this.members = members;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventMemberResponseDto> listMembers(Long eventId) {
        ensureEvent(eventId);
        return members.findByEvent_Id(eventId).stream().map(em -> {
            EventMemberResponseDto dto = new EventMemberResponseDto();
            dto.setUserId(em.getUser().getId());
            dto.setUsername(em.getUser().getUsername());
            dto.setEmail(em.getUser().getEmail());
            dto.setRole(em.getRole());
            dto.setAddedAt(em.getAddedAt());
            return dto;
        }).toList();
    }

    @Override
    public EventMemberResponseDto upsertMember(Long eventId, EventMemberUpsertDto dto) {
        Event event = ensureEvent(eventId);
        User user = users.findById(dto.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        EventMember.Pk pk = new EventMember.Pk(event.getId(), user.getId());
        EventMember em = members.findById(pk).orElseGet(() -> {
            EventMember x = new EventMember();
            x.setId(pk);
            x.setEvent(event);
            x.setUser(user);
            return x;
        });

        String role = dto.getRole();
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must not be blank");
        }
        if (role.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is too long");
        }

        em.setRole(role.trim());
        var saved = members.save(em);

        // Используем уже загруженные объекты user и event для избежания проблем с lazy loading
        var out = new EventMemberResponseDto();
        out.setUserId(user.getId());
        out.setUsername(user.getUsername());
        out.setEmail(user.getEmail());
        out.setRole(saved.getRole());
        out.setAddedAt(saved.getAddedAt());
        return out;
    }

    @Override
    public void removeMember(Long eventId, Long userId) {
        ensureEvent(eventId);
        EventMember.Pk pk = new EventMember.Pk(eventId, userId);
        if (!members.existsById(pk))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        members.deleteById(pk);
    }

    @Override
    public EventResponseDto createSubEvent(Long parentId, EventCreateDto dto) {
        Event parent = ensureEvent(parentId);
        Event child = EventMapper.toEntity(dto);
        child.setParent(parent);
        return EventMapper.toDto(events.save(child));
    }

    @Override
    public void attachChild(Long parentId, Long childId) {
        Event parent = ensureEvent(parentId);
        Event child = ensureEvent(childId);

        if (isAncestors(child, parent))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cycle detected");

        child.setParent(parent);
        events.save(child);
    }

    @Override
    public void detachChild(Long parentId, Long childId) {
        Event parent = ensureEvent(parentId);
        Event child = ensureEvent(childId);
        if (child.getParent() == null || !child.getParent().getId().equals(parent.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a direct child");

        child.setParent(null);
        events.save(child);
    }

    @Override
    @Transactional(readOnly = true)
    public EventTreeNodeDto getTree(Long eventId, int depth) {
        Event root = ensureEvent(eventId);
        return buildTree(root, Math.max(1, depth));
    }

    private Event ensureEvent(Long id) {
        return events.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private boolean isAncestors(Event candidateAncestor, Event node) {
        for (Event cur = node; cur != null; cur = cur.getParent()) {
            if (cur.getId().equals(candidateAncestor.getId()))
                return true;
        }
        return false;
    }

    private EventTreeNodeDto buildTree(Event root, int depth) {
        EventTreeNodeDto dto = new EventTreeNodeDto();
        dto.setId(root.getId());
        dto.setTitle(root.getTitle());
        dto.setStartTime(root.getStartTime());
        if (depth <= 1)
            return dto;
        var children = events.findByParentId(root.getId());
        List<EventTreeNodeDto> list = new ArrayList<>();
        for (Event ch : children)
            list.add(buildTree(ch, depth - 1));
        dto.setChildren(list);
        return dto;
    }
}