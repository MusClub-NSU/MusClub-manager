package com.nsu.musclub.domain;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "event_program_items")
public class EventProgramItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String artist;

    @Column(name = "planned_time")
    private LocalTime plannedTime;

    @Column(name = "duration_text", length = 32)
    private String durationText;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Integer position;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public LocalTime getPlannedTime() {
        return plannedTime;
    }

    public void setPlannedTime(LocalTime plannedTime) {
        this.plannedTime = plannedTime;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
