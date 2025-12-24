package com.nsu.musclub.mapper;

import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.user.*;

public class UserMapper {
    public static User toEntity(UserCreateDto dto) {
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setRole(dto.getRole());
        return u;
    }

    public static void update(UserUpdateDto dto, User u) {
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setRole(dto.getRole());
    }

    public static UserResponseDto toDto(User u) {
        UserResponseDto d = new UserResponseDto();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        d.setEmail(u.getEmail());
        d.setRole(u.getRole());
        d.setCreatedAt(u.getCreatedAt());
        return d;
    }
}
