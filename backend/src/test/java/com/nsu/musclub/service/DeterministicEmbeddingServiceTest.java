package com.nsu.musclub.service;

import com.nsu.musclub.service.impl.DeterministicEmbeddingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicEmbeddingServiceTest {

    @Test
    void embed_ShouldReturnDeterministicNormalizedVector() {
        DeterministicEmbeddingService service = new DeterministicEmbeddingService(64);

        float[] first = service.embed("Jazz Night");
        float[] second = service.embed("Jazz Night");

        assertEquals(64, first.length);
        assertArrayEquals(first, second);

        double l2 = 0.0;
        for (float value : first) {
            l2 += value * value;
        }
        assertTrue(l2 > 0.99 && l2 < 1.01);
    }

    @Test
    void embed_BlankText_ShouldReturnZeroVector() {
        DeterministicEmbeddingService service = new DeterministicEmbeddingService(16);

        float[] vector = service.embed("   ");

        assertEquals(16, vector.length);
        for (float value : vector) {
            assertEquals(0.0f, value);
        }
    }
}

