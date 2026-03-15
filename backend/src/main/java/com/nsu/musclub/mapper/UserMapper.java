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
        if (u.getAvatarData() != null && u.getAvatarData().length > 0) {
            d.setAvatarUrl("/api/users/" + u.getId() + "/avatar");
        }
        return d;
    }
}
