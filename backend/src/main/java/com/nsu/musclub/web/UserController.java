package com.nsu.musclub.web;

import com.nsu.musclub.dto.user.*;
import com.nsu.musclub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto create(@RequestBody @Valid UserCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public UserResponseDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public Page<UserResponseDto> list(@ParameterObject Pageable pageable) {
        return service.list(pageable);
    }

    @PutMapping("/{id}")
    public UserResponseDto update(@PathVariable Long id, @RequestBody @Valid UserUpdateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
