package com.nsu.musclub.service;

public interface EmbeddingService {
    float[] embed(String text);

    int dimensions();
}

