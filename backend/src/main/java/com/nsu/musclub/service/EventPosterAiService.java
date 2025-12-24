package com.nsu.musclub.service;

public interface EventPosterAiService {
    String generatePosterDescription(Long eventId, boolean saveToEvent);
}
