package com.nsu.musclub.dto.user;

import jakarta.validation.constraints.Size;

public class UserPasswordUpdateDto {
    @Size(min = 8, max = 255)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

