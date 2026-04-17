package com.nsu.musclub.web;

import com.nsu.musclub.dto.user.*;
import com.nsu.musclub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

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

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@PathVariable Long id, @RequestBody @Valid UserPasswordUpdateDto dto) {
        service.updatePassword(id, dto);
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponseDto uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.uploadAvatar(id, file);
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        UserAvatarDto avatar = service.getAvatar(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (avatar.getContentType() != null && !avatar.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(avatar.getContentType());
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(mediaType);
        if (avatar.getFileName() != null && !avatar.getFileName().isBlank()) {
            response.header(
                    "Content-Disposition",
                    ContentDisposition.inline().filename(avatar.getFileName(), StandardCharsets.UTF_8).build().toString()
            );
        }
        return response.body(avatar.getData());
    }

    @DeleteMapping("/{id}/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(@PathVariable Long id) {
        service.deleteAvatar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
