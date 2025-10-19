package com.nsu.musclub.repository;

import com.nsu.musclub.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
