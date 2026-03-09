package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.user.*;
import com.nsu.musclub.exception.ResourceAlreadyExistsException;
import com.nsu.musclub.exception.ResourceNotFoundException;
import com.nsu.musclub.mapper.UserMapper;
import com.nsu.musclub.repository.UserRepository;
import com.nsu.musclub.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository users;

    public UserServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserResponseDto create(UserCreateDto dto) {
        if (users.existsByUsername(dto.getUsername())) {
            throw new ResourceAlreadyExistsException("Пользователь", "username", dto.getUsername());
        }
        if (users.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Пользователь", "email", dto.getEmail());
        }
        return UserMapper.toDto(users.save(UserMapper.toEntity(dto)));
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

        if (!u.getUsername().equals(dto.getUsername()) && users.existsByUsername(dto.getUsername())) {
            throw new ResourceAlreadyExistsException("Пользователь", "username", dto.getUsername());
        }
        if (!u.getEmail().equals(dto.getEmail()) && users.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("Пользователь", "email", dto.getEmail());
        }

        UserMapper.update(dto, u);
        return UserMapper.toDto(users.save(u));
    }

    @Override
    public void delete(Long id) {
        if (!users.existsById(id)) {
            throw new ResourceNotFoundException("Пользователь", id);
        }
        users.deleteById(id);
    }
}
