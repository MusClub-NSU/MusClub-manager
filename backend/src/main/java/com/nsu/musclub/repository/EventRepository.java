package com.nsu.musclub.repository;

import com.nsu.musclub.domain.Event;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByParentId(Long parentId);
}
