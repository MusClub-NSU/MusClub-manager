package com.nsu.musclub.service;

import com.nsu.musclub.domain.Event;
import com.nsu.musclub.domain.User;

public interface SearchIndexingService {
    void indexEvent(Event event);

    void indexUser(User user);

    void removeEvent(Long eventId);

    void removeUser(Long userId);

    void rebuildIndex();
}

