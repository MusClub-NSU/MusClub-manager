package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.user.*;
import com.nsu.musclub.mapper.UserMapper;
import com.nsu.musclub.repository.UserRepository;
import com.nsu.musclub.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository users;

    public UserServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserResponseDto create(UserCreateDto dto) {
        if (users.existsByUsername(dto.getUsername()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        if (users.existsByEmail(dto.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        return UserMapper.toDto(users.save(UserMapper.toEntity(dto)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto get(Long id) {
        return users.findById(id).map(UserMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> list(Pageable pageable) {
        return users.findAll(pageable).map(UserMapper::toDto);
    }

    @Override
    public UserResponseDto update(Long id, UserUpdateDto dto) {
        var u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!u.getUsername().equals(dto.getUsername()) && users.existsByUsername(dto.getUsername()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        if (!u.getEmail().equals(dto.getEmail()) && users.existsByEmail(dto.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        UserMapper.update(dto, u);
        return UserMapper.toDto(users.save(u));
    }

    @Override
    public void delete(Long id) {
        if (!users.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        users.deleteById(id);
    }
}
