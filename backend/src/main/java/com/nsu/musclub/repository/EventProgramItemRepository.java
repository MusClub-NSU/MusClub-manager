package com.nsu.musclub.repository;

import com.nsu.musclub.domain.EventProgramItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventProgramItemRepository extends JpaRepository<EventProgramItem, Long> {
    List<EventProgramItem> findByEvent_IdOrderByPositionAsc(Long eventId);

    Optional<EventProgramItem> findByIdAndEvent_Id(Long id, Long eventId);
}
