package com.nsu.musclub.service;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.event.EventCreateDto;
import com.nsu.musclub.dto.event.EventMemberResponseDto;
import com.nsu.musclub.dto.event.EventMemberUpsertDto;
import com.nsu.musclub.dto.event.EventResponseDto;
import com.nsu.musclub.dto.event.EventTreeNodeDto;
import com.nsu.musclub.dto.user.UserCreateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.transaction.annotation.Transactional
class EventRelationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private EventRelationService relationService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    private Long eventId;
    private Long userId1;
    private Long userId2;
    private Long userId3;

    @BeforeEach
    void setUp() {
        EventCreateDto eventDto = new EventCreateDto();
        eventDto.setTitle("Test Event");
        eventDto.setStartTime(OffsetDateTime.now().plusDays(1));
        EventResponseDto event = eventService.create(eventDto);
        eventId = event.getId();

        UserCreateDto userDto1 = new UserCreateDto();
        userDto1.setUsername("user1");
        userDto1.setEmail("user1@example.com");
        userDto1.setRole("MEMBER");
        userId1 = userService.create(userDto1).getId();

        UserCreateDto userDto2 = new UserCreateDto();
        userDto2.setUsername("user2");
        userDto2.setEmail("user2@example.com");
        userDto2.setRole("MEMBER");
        userId2 = userService.create(userDto2).getId();

        UserCreateDto userDto3 = new UserCreateDto();
        userDto3.setUsername("user3");
        userDto3.setEmail("user3@example.com");
        userDto3.setRole("MEMBER");
        userId3 = userService.create(userDto3).getId();
    }

    @Test
    void listMembers_ShouldReturnEmptyList() {
        List<EventMemberResponseDto> members = relationService.listMembers(eventId);
        assertTrue(members.isEmpty());
    }

    @Test
    void listMembers_WithMembers_ShouldReturnAllMembers() {
        EventMemberUpsertDto dto1 = new EventMemberUpsertDto();
        dto1.setUserId(userId1);
        dto1.setRole("ORGANIZER");
        relationService.upsertMember(eventId, dto1);

        EventMemberUpsertDto dto2 = new EventMemberUpsertDto();
        dto2.setUserId(userId2);
        dto2.setRole("PERFORMER");
        relationService.upsertMember(eventId, dto2);

        List<EventMemberResponseDto> members = relationService.listMembers(eventId);
        assertEquals(2, members.size());
    }

    @Test
    void listMembers_WithNonExistentEvent_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.listMembers(99999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void upsertMember_ShouldAddNewMember() {
        EventMemberUpsertDto dto = new EventMemberUpsertDto();
        dto.setUserId(userId1);
        dto.setRole("ORGANIZER");

        EventMemberResponseDto result = relationService.upsertMember(eventId, dto);

        assertEquals(userId1, result.getUserId());
        assertEquals("ORGANIZER", result.getRole());
        assertNotNull(result.getAddedAt());
    }

    @Test
    void upsertMember_ShouldUpdateExistingMember() {
        EventMemberUpsertDto dto1 = new EventMemberUpsertDto();
        dto1.setUserId(userId1);
        dto1.setRole("PERFORMER");
        relationService.upsertMember(eventId, dto1);

        EventMemberUpsertDto dto2 = new EventMemberUpsertDto();
        dto2.setUserId(userId1);
        dto2.setRole("ORGANIZER");

        EventMemberResponseDto result = relationService.upsertMember(eventId, dto2);

        assertEquals(userId1, result.getUserId());
        assertEquals("ORGANIZER", result.getRole());
    }

    @Test
    void upsertMember_WithNonExistentEvent_ShouldThrow404() {
        EventMemberUpsertDto dto = new EventMemberUpsertDto();
        dto.setUserId(userId1);
        dto.setRole("ORGANIZER");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.upsertMember(99999L, dto);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void upsertMember_WithNonExistentUser_ShouldThrow404() {
        EventMemberUpsertDto dto = new EventMemberUpsertDto();
        dto.setUserId(99999L);
        dto.setRole("ORGANIZER");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.upsertMember(eventId, dto);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void upsertMember_WithInvalidRole_ShouldThrow400() {
        EventMemberUpsertDto dto = new EventMemberUpsertDto();
        dto.setUserId(userId1);
        dto.setRole("INVALID_ROLE");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.upsertMember(eventId, dto);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void upsertMember_WithAllRoles_ShouldSucceed() {
        EventMemberUpsertDto organizerDto = new EventMemberUpsertDto();
        organizerDto.setUserId(userId1);
        organizerDto.setRole("ORGANIZER");
        relationService.upsertMember(eventId, organizerDto);

        EventMemberUpsertDto performerDto = new EventMemberUpsertDto();
        performerDto.setUserId(userId2);
        performerDto.setRole("PERFORMER");
        relationService.upsertMember(eventId, performerDto);

        EventMemberUpsertDto volunteerDto = new EventMemberUpsertDto();
        volunteerDto.setUserId(userId3);
        volunteerDto.setRole("VOLUNTEER");
        relationService.upsertMember(eventId, volunteerDto);

        List<EventMemberResponseDto> members = relationService.listMembers(eventId);
        assertEquals(3, members.size());
    }

    @Test
    void removeMember_ShouldSucceed() {
        EventMemberUpsertDto dto = new EventMemberUpsertDto();
        dto.setUserId(userId1);
        dto.setRole("ORGANIZER");
        relationService.upsertMember(eventId, dto);

        relationService.removeMember(eventId, userId1);

        List<EventMemberResponseDto> members = relationService.listMembers(eventId);
        assertTrue(members.isEmpty());
    }

    @Test
    void removeMember_WithNonExistentEvent_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.removeMember(99999L, userId1);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void removeMember_WithNonExistentMember_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.removeMember(eventId, userId1);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void createSubEvent_ShouldSucceed() {
        EventCreateDto subEventDto = new EventCreateDto();
        subEventDto.setTitle("Sub Event");
        subEventDto.setStartTime(OffsetDateTime.now().plusDays(2));

        EventResponseDto result = relationService.createSubEvent(eventId, subEventDto);

        assertNotNull(result.getId());
        assertEquals("Sub Event", result.getTitle());
        EventResponseDto parent = eventService.get(eventId);
        assertNotNull(parent);
    }

    @Test
    void createSubEvent_WithNonExistentParent_ShouldThrow404() {
        EventCreateDto subEventDto = new EventCreateDto();
        subEventDto.setTitle("Sub Event");
        subEventDto.setStartTime(OffsetDateTime.now().plusDays(2));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.createSubEvent(99999L, subEventDto);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void attachChild_ShouldSucceed() {
        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child = eventService.create(childDto);

        relationService.attachChild(eventId, child.getId());

        EventResponseDto updatedChild = eventService.get(child.getId());
        assertNotNull(updatedChild);
    }

    @Test
    void attachChild_WithNonExistentParent_ShouldThrow404() {
        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child = eventService.create(childDto);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.attachChild(99999L, child.getId());
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void attachChild_WithNonExistentChild_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.attachChild(eventId, 99999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void attachChild_WithCycle_ShouldThrow400() {
        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child = eventService.create(childDto);

        relationService.attachChild(eventId, child.getId());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.attachChild(child.getId(), eventId);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Cycle detected"));
    }

    @Test
    void attachChild_WithDeepCycle_ShouldThrow400() {
        EventCreateDto child1Dto = new EventCreateDto();
        child1Dto.setTitle("Child 1");
        child1Dto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child1 = eventService.create(child1Dto);

        EventCreateDto child2Dto = new EventCreateDto();
        child2Dto.setTitle("Child 2");
        child2Dto.setStartTime(OffsetDateTime.now().plusDays(3));
        EventResponseDto child2 = eventService.create(child2Dto);

        relationService.attachChild(eventId, child1.getId());
        relationService.attachChild(child1.getId(), child2.getId());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.attachChild(child2.getId(), eventId);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Cycle detected"));
    }

    @Test
    void detachChild_ShouldSucceed() {
        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child = eventService.create(childDto);

        relationService.attachChild(eventId, child.getId());
        relationService.detachChild(eventId, child.getId());

        EventResponseDto updatedChild = eventService.get(child.getId());
        assertNotNull(updatedChild);
    }

    @Test
    void detachChild_WithNonExistentParent_ShouldThrow404() {
        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child = eventService.create(childDto);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.detachChild(99999L, child.getId());
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void detachChild_WithNonExistentChild_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.detachChild(eventId, 99999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void detachChild_WithNotDirectChild_ShouldThrow400() {
        EventCreateDto childDto = new EventCreateDto();
        childDto.setTitle("Child Event");
        childDto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child = eventService.create(childDto);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.detachChild(eventId, child.getId());
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Not a direct child"));
    }

    @Test
    void getTree_ShouldReturnRootOnly() {
        EventTreeNodeDto tree = relationService.getTree(eventId, 1);

        assertEquals(eventId, tree.getId());
        assertEquals("Test Event", tree.getTitle());
        assertTrue(tree.getChildren().isEmpty());
    }

    @Test
    void getTree_WithChildren_ShouldReturnTree() {
        EventCreateDto child1Dto = new EventCreateDto();
        child1Dto.setTitle("Child 1");
        child1Dto.setStartTime(OffsetDateTime.now().plusDays(2));
        relationService.createSubEvent(eventId, child1Dto);

        EventCreateDto child2Dto = new EventCreateDto();
        child2Dto.setTitle("Child 2");
        child2Dto.setStartTime(OffsetDateTime.now().plusDays(3));
        relationService.createSubEvent(eventId, child2Dto);

        EventTreeNodeDto tree = relationService.getTree(eventId, 2);

        assertEquals(eventId, tree.getId());
        assertEquals(2, tree.getChildren().size());
    }

    @Test
    void getTree_WithDepthLimit_ShouldRespectDepth() {
        EventCreateDto child1Dto = new EventCreateDto();
        child1Dto.setTitle("Child 1");
        child1Dto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child1 = relationService.createSubEvent(eventId, child1Dto);

        EventCreateDto grandchildDto = new EventCreateDto();
        grandchildDto.setTitle("Grandchild");
        grandchildDto.setStartTime(OffsetDateTime.now().plusDays(3));
        relationService.createSubEvent(child1.getId(), grandchildDto);

        EventTreeNodeDto tree = relationService.getTree(eventId, 2);

        assertEquals(eventId, tree.getId());
        assertEquals(1, tree.getChildren().size());
        assertTrue(tree.getChildren().get(0).getChildren().isEmpty());
    }

    @Test
    void getTree_WithNonExistentEvent_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            relationService.getTree(99999L, 3);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getTree_WithComplexHierarchy_ShouldReturnCorrectStructure() {
        EventCreateDto child1Dto = new EventCreateDto();
        child1Dto.setTitle("Child 1");
        child1Dto.setStartTime(OffsetDateTime.now().plusDays(2));
        EventResponseDto child1 = relationService.createSubEvent(eventId, child1Dto);

        EventCreateDto child2Dto = new EventCreateDto();
        child2Dto.setTitle("Child 2");
        child2Dto.setStartTime(OffsetDateTime.now().plusDays(3));
        relationService.createSubEvent(eventId, child2Dto);

        EventCreateDto grandchildDto = new EventCreateDto();
        grandchildDto.setTitle("Grandchild");
        grandchildDto.setStartTime(OffsetDateTime.now().plusDays(4));
        relationService.createSubEvent(child1.getId(), grandchildDto);

        EventTreeNodeDto tree = relationService.getTree(eventId, 3);

        assertEquals(eventId, tree.getId());
        assertEquals(2, tree.getChildren().size());
        assertEquals(1, tree.getChildren().get(0).getChildren().size());
        assertEquals(0, tree.getChildren().get(1).getChildren().size());
    }
}

