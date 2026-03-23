package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.user.*;
import com.nsu.musclub.exception.BadRequestException;
import com.nsu.musclub.exception.ResourceAlreadyExistsException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.mapper.UserMapper;
import com.nsu.musclub.repository.UserRepository;
import com.nsu.musclub.service.SearchIndexingService;
import com.nsu.musclub.service.KeycloakUserProvisioningService;
import com.nsu.musclub.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024;

    private final UserRepository users;
    private final SearchIndexingService searchIndexingService;
    private final KeycloakUserProvisioningService keycloakUserProvisioningService;

    public UserServiceImpl(UserRepository users,
                           SearchIndexingService searchIndexingService,
                           KeycloakUserProvisioningService keycloakUserProvisioningService) {
        this.users = users;
        this.searchIndexingService = searchIndexingService;
        this.keycloakUserProvisioningService = keycloakUserProvisioningService;
    }

    @Override
    public UserResponseDto create(UserCreateDto dto) {
        if (users.existsByUsername(dto.getUsername())) {
            throw new ResourceAlreadyExistsException("Пользователь", "username", dto.getUsername());
        }
        if (users.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Пользователь", "email", dto.getEmail());
        }

        // 1) Provision user in Keycloak
        String keycloakUserId = keycloakUserProvisioningService.createUserAndAssignRole(dto);

        // 2) Save user in our DB (rollback best-effort if something fails)
        try {
            User created = users.save(UserMapper.toEntity(dto));
            searchIndexingService.indexUser(created);
            return UserMapper.toDto(created);
        } catch (RuntimeException ex) {
            keycloakUserProvisioningService.deleteUserQuietly(keycloakUserId);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto get(Long id) {
        return users.findById(id).map(UserMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> list(Pageable pageable) {
        return users.findAll(pageable).map(UserMapper::toDto);
    }

    @Override
    public UserResponseDto update(Long id, UserUpdateDto dto) {
        var u = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        // UI may send partial updates (e.g. only username). Keep existing fields if omitted.
        if (dto.getUsername() == null) dto.setUsername(u.getUsername());
        if (dto.getEmail() == null) dto.setEmail(u.getEmail());
        if (dto.getRole() == null) dto.setRole(u.getRole());

        if (dto.getUsername().isBlank()) {
            throw new BadRequestException("Имя пользователя обязательно", "USERNAME_REQUIRED");
        }
        if (dto.getEmail().isBlank()) {
            throw new BadRequestException("Email обязателен", "EMAIL_REQUIRED");
        }
        if (dto.getRole().isBlank()) {
            throw new BadRequestException("Роль обязательна", "ROLE_REQUIRED");
        }

        if (!u.getUsername().equals(dto.getUsername()) && users.existsByUsername(dto.getUsername())) {
            throw new ResourceAlreadyExistsException("Пользователь", "username", dto.getUsername());
        }
        if (!u.getEmail().equals(dto.getEmail()) && users.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Пользователь", "email", dto.getEmail());
        }

        UserMapper.update(dto, u);
        User updated = users.save(u);
        searchIndexingService.indexUser(updated);
        return UserMapper.toDto(updated);
    }

    @Override
    public UserResponseDto uploadAvatar(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл аватара пустой", "EMPTY_AVATAR_FILE");
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new BadRequestException("Размер аватара превышает 5MB", "AVATAR_TOO_LARGE");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Поддерживаются только изображения", "INVALID_AVATAR_CONTENT_TYPE");
        }

        User user = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        try {
            user.setAvatarData(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать файл аватара", "AVATAR_READ_ERROR");
        }
        user.setAvatarContentType(contentType);
        user.setAvatarFileName(file.getOriginalFilename());
        User updated = users.save(user);
        searchIndexingService.indexUser(updated);
        return UserMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAvatarDto getAvatar(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        if (user.getAvatarData() == null || user.getAvatarData().length == 0) {
            throw new ResourceNotFoundException("Аватар пользователя", id);
        }

        UserAvatarDto dto = new UserAvatarDto();
        dto.setData(user.getAvatarData());
        dto.setContentType(user.getAvatarContentType());
        dto.setFileName(user.getAvatarFileName());
        return dto;
    }

    @Override
    public void deleteAvatar(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));

        user.setAvatarData(null);
        user.setAvatarContentType(null);
        user.setAvatarFileName(null);
        User updated = users.save(user);
        searchIndexingService.indexUser(updated);
    }

    @Override
    public void delete(Long id) {
        if (!users.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь", id);
        }
        users.deleteById(id);
        searchIndexingService.removeUser(id);
    }
}
