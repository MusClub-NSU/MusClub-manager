package com.nsu.musclub.dto.event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventTreeNodeDto {
    private Long id;
    private String title;
    private OffsetDateTime startTime;
    private List<EventTreeNodeDto> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public List<EventTreeNodeDto> getChildren() {
        return children;
    }

    public void setChildren(List<EventTreeNodeDto> children) {
        this.children = children;
    }
}
