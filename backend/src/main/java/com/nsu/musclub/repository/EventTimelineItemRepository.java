package com.nsu.musclub.repository;

import com.nsu.musclub.domain.EventTimelineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventTimelineItemRepository extends JpaRepository<EventTimelineItem, Long> {
    List<EventTimelineItem> findByEvent_IdOrderByPositionAsc(Long eventId);

    Optional<EventTimelineItem> findByIdAndEvent_Id(Long id, Long eventId);
}
