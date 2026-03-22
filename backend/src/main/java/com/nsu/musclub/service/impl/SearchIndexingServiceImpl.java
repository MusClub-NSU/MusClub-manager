package com.nsu.musclub.service.impl;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.domain.User;
import com.nsu.musclub.dto.search.SearchEntityType;
import com.nsu.musclub.repository.EventRepository;
import com.nsu.musclub.repository.SearchRepository;
import com.nsu.musclub.repository.UserRepository;
import com.nsu.musclub.service.EmbeddingService;
import com.nsu.musclub.service.SearchIndexingService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SearchIndexingServiceImpl implements SearchIndexingService {
    private final SearchRepository searchRepository;
    private final EmbeddingService embeddingService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public SearchIndexingServiceImpl(SearchRepository searchRepository,
                                     EmbeddingService embeddingService,
                                     EventRepository eventRepository,
                                     UserRepository userRepository) {
        this.searchRepository = searchRepository;
        this.embeddingService = embeddingService;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void indexEvent(Event event) {
        if (event == null || event.getId() == null) {
            return;
        }
        String title = safe(event.getTitle());
        String content = joinNonBlank(
                event.getDescription(),
                event.getVenue(),
                event.getAiDescription(),
                event.getStatus() == null ? null : event.getStatus().name()
        );
        String embedding = toVectorLiteral(embeddingService.embed(title + " " + content));
        searchRepository.upsertDocument(SearchEntityType.EVENT, event.getId(), title, content, embedding);
    }

    @Override
    public void indexUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String title = safe(user.getUsername());
        String content = joinNonBlank(user.getEmail(), user.getRole());
        String embedding = toVectorLiteral(embeddingService.embed(title + " " + content));
        searchRepository.upsertDocument(SearchEntityType.USER, user.getId(), title, content, embedding);
    }

    @Override
    public void removeEvent(Long eventId) {
        if (eventId != null) {
            searchRepository.deleteDocument(SearchEntityType.EVENT, eventId);
        }
    }

    @Override
    public void removeUser(Long userId) {
        if (userId != null) {
            searchRepository.deleteDocument(SearchEntityType.USER, userId);
        }
    }

    @Override
    @Transactional
    public void rebuildIndex() {
        eventRepository.findAll().forEach(this::indexEvent);
        userRepository.findAll().forEach(this::indexUser);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmupSearchIndex() {
        rebuildIndex();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value.trim());
            }
        }
        return String.join(" ", parts);
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}

