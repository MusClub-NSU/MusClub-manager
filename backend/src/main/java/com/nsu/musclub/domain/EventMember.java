package com.nsu.musclub.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "event_members")
public class EventMember {
    public static class Pk implements Serializable {
        @Column(name = "event_id")
        private Long eventId;

        @Column(name = "user_id")
        private Long userId;

        public Pk() {
        }

        public Pk(Long eventId, Long userId) {
            this.eventId = eventId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (!(obj instanceof Pk pk)) {
                return false;
            }

            return Objects.equals(this.eventId, pk.eventId)
                    && Objects.equals(this.userId, pk.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, userId);
        }
    }

    @EmbeddedId
    private Pk id = new Pk();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64)
    private String role;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();

    public Pk getId() {
        return id;
    }

    public void setId(Pk id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }
 
    public void setRole(String role) {
        this.role = role;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(OffsetDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
