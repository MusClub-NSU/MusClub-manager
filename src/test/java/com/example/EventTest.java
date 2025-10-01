package com.example;

import java.time.LocalDateTime;

public class EventTest {
    public static void main(String[] args) {
        // 1. 创建新事件 / Create new events
        EventCRUD.createEvent("Meeting Test", "Project kickoff meeting", LocalDateTime.now().plusDays(1), "Room A101");
        EventCRUD.createEvent("Product Launch", "New version release", LocalDateTime.now().plusWeeks(1), "Auditorium");

        // 2. 查询所有 / Query all
        EventCRUD.getAllEvents();

        // 3. 按ID查询 / Query by ID
        EventCRUD.getEventById(1);

        // 4. 更新事件 / Update event
        EventCRUD.updateEvent(1, "Meeting Test (Updated)", "Updated description", LocalDateTime.now().plusDays(2), "Room B202");

        // 5. 删除事件 / Delete event
        EventCRUD.deleteEvent(2);

        // 6. 再次查询所有 / Query all again
        EventCRUD.getAllEvents();
    }
}