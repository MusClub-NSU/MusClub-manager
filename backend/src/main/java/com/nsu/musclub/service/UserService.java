package com.nsu.musclub.service;

import com.nsu.musclub.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponseDto create(UserCreateDto dto);

    UserResponseDto get(Long id);

    Page<UserResponseDto> list(Pageable pageable);

    UserResponseDto update(Long id, UserUpdateDto dto);

    UserResponseDto uploadAvatar(Long id, MultipartFile file);

    UserAvatarDto getAvatar(Long id);

    void deleteAvatar(Long id);

    void delete(Long id);
}
