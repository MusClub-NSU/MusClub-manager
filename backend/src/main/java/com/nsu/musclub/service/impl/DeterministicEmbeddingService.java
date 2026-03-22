package com.nsu.musclub.service.impl;

import com.nsu.musclub.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class DeterministicEmbeddingService implements EmbeddingService {
    private final int dimensions;

    public DeterministicEmbeddingService(@Value("${search.embedding-dimensions:256}") int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String normalized = normalize(text);
        if (normalized.length() < 3) {
            normalized = normalized + "___";
        }

        for (int i = 0; i < normalized.length() - 2; i++) {
            String trigram = normalized.substring(i, i + 3);
            int bucket = Math.floorMod(trigram.hashCode(), dimensions);
            vector[bucket] += 1.0f;
        }

        normalizeL2(vector);
        return vector;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void normalizeL2(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }
        if (sum == 0.0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}

