package com.nsu.musclub.repository;

import com.nsu.musclub.domain.EventMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventMemberRepository extends JpaRepository<EventMember, EventMember.Pk> {
    List<EventMember> findByEvent_Id(Long eventId);
    List<EventMember> findByUser_Id(Long userId);
    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);
}
