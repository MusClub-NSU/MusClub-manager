package com.nsu.musclub.repository;

import com.nsu.musclub.AbstractIntegrationTest;
import com.nsu.musclub.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveUser_ShouldSucceed() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole("MEMBER");

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getUsername());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("MEMBER", saved.getRole());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findById_ShouldReturnUser() {
        User user = new User();
        user.setUsername("finduser");
        user.setEmail("find@example.com");
        user.setRole("MEMBER");

        User saved = userRepository.save(user);
        User found = userRepository.findById(saved.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals("finduser", found.getUsername());
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        assertFalse(userRepository.findById(99999L).isPresent());
    }

    @Test
    void existsByUsername_ShouldReturnTrue() {
        User user = new User();
        user.setUsername("existsuser");
        user.setEmail("exists@example.com");
        user.setRole("MEMBER");

        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("existsuser"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        User user = new User();
        user.setUsername("emailuser");
        user.setEmail("exists@example.com");
        user.setRole("MEMBER");

        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exists@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void saveUser_WithDuplicateUsername_ShouldFail() {
        User user1 = new User();
        user1.setUsername("duplicate");
        user1.setEmail("user1@example.com");
        user1.setRole("MEMBER");

        User user2 = new User();
        user2.setUsername("duplicate");
        user2.setEmail("user2@example.com");
        user2.setRole("MEMBER");

        userRepository.save(user1);

        assertThrows(Exception.class, () -> {
            userRepository.save(user2);
            userRepository.flush();
        });
    }

    @Test
    void saveUser_WithDuplicateEmail_ShouldFail() {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("duplicate@example.com");
        user1.setRole("MEMBER");

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("duplicate@example.com");
        user2.setRole("MEMBER");

        userRepository.save(user1);

        assertThrows(Exception.class, () -> {
            userRepository.save(user2);
            userRepository.flush();
        });
    }

    @Test
    void deleteById_ShouldRemoveUser() {
        User user = new User();
        user.setUsername("deleteuser");
        user.setEmail("delete@example.com");
        user.setRole("MEMBER");

        User saved = userRepository.save(user);
        Long id = saved.getId();

        userRepository.deleteById(id);

        assertFalse(userRepository.existsById(id));
    }

    @Test
    void existsById_ShouldReturnCorrectValue() {
        User user = new User();
        user.setUsername("existuser");
        user.setEmail("exist@example.com");
        user.setRole("MEMBER");

        User saved = userRepository.save(user);

        assertTrue(userRepository.existsById(saved.getId()));
        assertFalse(userRepository.existsById(99999L));
    }

    @Test
    void saveUser_ShouldSetCreatedAt() {
        User user = new User();
        user.setUsername("timeuser");
        user.setEmail("time@example.com");
        user.setRole("MEMBER");

        User saved = userRepository.save(user);

        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
        assertTrue(saved.getCreatedAt().isAfter(OffsetDateTime.now().minusSeconds(5)));
    }
}

