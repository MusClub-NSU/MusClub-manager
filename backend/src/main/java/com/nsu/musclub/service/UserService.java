package com.nsu.musclub.service;

import com.nsu.musclub.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseDto create(UserCreateDto dto);

    UserResponseDto get(Long id);

    Page<UserResponseDto> list(Pageable pageable);

    UserResponseDto update(Long id, UserUpdateDto dto);

    void delete(Long id);
}
