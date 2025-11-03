package com.nsu.musclub.service;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.dto.user.UserCreateDto;
import com.nsu.musclub.dto.user.UserResponseDto;
import com.nsu.musclub.dto.user.UserUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.transaction.annotation.Transactional
class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void createUser_ShouldSucceed() {
        UserCreateDto dto = new UserCreateDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setRole("MEMBER");

        UserResponseDto result = userService.create(dto);

        assertNotNull(result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("MEMBER", result.getRole());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldThrow409() {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setUsername("duplicate");
        dto1.setEmail("user1@example.com");
        dto1.setRole("MEMBER");

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setUsername("duplicate");
        dto2.setEmail("user2@example.com");
        dto2.setRole("MEMBER");

        userService.create(dto1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.create(dto2);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Username already in use"));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrow409() {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setUsername("user1");
        dto1.setEmail("duplicate@example.com");
        dto1.setRole("MEMBER");

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setUsername("user2");
        dto2.setEmail("duplicate@example.com");
        dto2.setRole("MEMBER");

        userService.create(dto1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.create(dto2);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Email already in use"));
    }

    @Test
    void getUser_ShouldReturnUser() {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("getuser");
        createDto.setEmail("getuser@example.com");
        createDto.setRole("MEMBER");

        UserResponseDto created = userService.create(createDto);
        UserResponseDto found = userService.get(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("getuser", found.getUsername());
        assertEquals("getuser@example.com", found.getEmail());
    }

    @Test
    void getUser_WithNonExistentId_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.get(99999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void listUsers_ShouldReturnPage() {
        // Create multiple users
        for (int i = 1; i <= 5; i++) {
            UserCreateDto dto = new UserCreateDto();
            dto.setUsername("user" + i);
            dto.setEmail("user" + i + "@example.com");
            dto.setRole("MEMBER");
            userService.create(dto);
        }

        Page<UserResponseDto> page = userService.list(Pageable.ofSize(10));

        assertTrue(page.getTotalElements() >= 5);
        assertEquals(10, page.getSize());
    }

    @Test
    void updateUser_ShouldSucceed() {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("original");
        createDto.setEmail("original@example.com");
        createDto.setRole("MEMBER");

        UserResponseDto created = userService.create(createDto);

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("updated");
        updateDto.setEmail("updated@example.com");
        updateDto.setRole("ORGANIZER");

        UserResponseDto updated = userService.update(created.getId(), updateDto);

        assertEquals(created.getId(), updated.getId());
        assertEquals("updated", updated.getUsername());
        assertEquals("updated@example.com", updated.getEmail());
        assertEquals("ORGANIZER", updated.getRole());
    }

    @Test
    void updateUser_WithNonExistentId_ShouldThrow404() {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("updated");
        updateDto.setEmail("updated@example.com");
        updateDto.setRole("MEMBER");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.update(99999L, updateDto);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateUser_WithDuplicateUsername_ShouldThrow409() {
        UserCreateDto dto1 = new UserCreateDto();
        dto1.setUsername("user1");
        dto1.setEmail("user1@example.com");
        dto1.setRole("MEMBER");

        UserCreateDto dto2 = new UserCreateDto();
        dto2.setUsername("user2");
        dto2.setEmail("user2@example.com");
        dto2.setRole("MEMBER");

        userService.create(dto1);
        UserResponseDto created2 = userService.create(dto2);

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("user1"); // Duplicate
        updateDto.setEmail("user2@example.com");
        updateDto.setRole("MEMBER");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.update(created2.getId(), updateDto);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Username already in use"));
    }

    @Test
    void updateUser_WithSameUsername_ShouldSucceed() {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("sameuser");
        createDto.setEmail("sameuser@example.com");
        createDto.setRole("MEMBER");

        UserResponseDto created = userService.create(createDto);

        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setUsername("sameuser"); // Same username is OK
        updateDto.setEmail("updated@example.com");
        updateDto.setRole("ORGANIZER");

        UserResponseDto updated = userService.update(created.getId(), updateDto);

        assertEquals("sameuser", updated.getUsername());
        assertEquals("updated@example.com", updated.getEmail());
    }

    @Test
    void deleteUser_ShouldSucceed() {
        UserCreateDto createDto = new UserCreateDto();
        createDto.setUsername("deleteuser");
        createDto.setEmail("deleteuser@example.com");
        createDto.setRole("MEMBER");

        UserResponseDto created = userService.create(createDto);
        Long id = created.getId();

        userService.delete(id);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.get(id);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteUser_WithNonExistentId_ShouldThrow404() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.delete(99999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
