package com.nsu.musclub.dto.push;

/**
 * DTO для VAPID публичного ключа (для клиента)
 */
public class VapidPublicKeyDto {

    private String publicKey;

    public VapidPublicKeyDto() {
    }

    public VapidPublicKeyDto(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
