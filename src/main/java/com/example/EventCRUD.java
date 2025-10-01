package com.example;

import java.sql.*;
import java.time.LocalDateTime;

public class EventCRUD {
    // 数据库连接配置 / Database connection configuration
    static final String URL = "jdbc:mysql://localhost:3306/club_demo?useSSL=false&serverTimezone=UTC";
    static final String USER = "root";
    static final String PASSWORD = "123456";

    // 创建事件 / Create event
    public static void createEvent(String title, String description, LocalDateTime eventTime, String location) {
        String sql = "INSERT INTO events (title, description, event_time, location) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, title);
            stmt.setString(2, description);
            if (eventTime != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(eventTime));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }
            stmt.setString(4, location);

            int rows = stmt.executeUpdate();
            System.out.println("Event created successfully, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询所有事件 / Query all events
    public static void getAllEvents() {
        String sql = "SELECT * FROM events ORDER BY id";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n📋 All events list:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id")
                        + " | Title: " + rs.getString("title")
                        + " | Description: " + rs.getString("description")
                        + " | Time: " + rs.getTimestamp("event_time")
                        + " | Location: " + rs.getString("location")
                        + " | Created at: " + rs.getTimestamp("created_at")
                        + " | Updated at: " + rs.getTimestamp("updated_at"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 按ID查询事件 / Query event by ID
    public static void getEventById(long id) {
        String sql = "SELECT * FROM events WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n🔍 Query result:");
                    System.out.println("ID: " + rs.getLong("id")
                            + " | Title: " + rs.getString("title")
                            + " | Description: " + rs.getString("description")
                            + " | Time: " + rs.getTimestamp("event_time")
                            + " | Location: " + rs.getString("location")
                            + " | Created at: " + rs.getTimestamp("created_at")
                            + " | Updated at: " + rs.getTimestamp("updated_at"));
                } else {
                    System.out.println("⚠️ Event with ID = " + id + " not found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新事件（按ID）/ Update event (by ID)
    public static void updateEvent(long id, String newTitle, String newDesc, LocalDateTime newTime, String newLocation) {
        String sql = "UPDATE events SET title = ?, description = ?, event_time = ?, location = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newTitle);
            stmt.setString(2, newDesc);
            if (newTime != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(newTime));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }
            stmt.setString(4, newLocation);
            stmt.setLong(5, id);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Event with ID = " + id + " updated successfully.");
            } else {
                System.out.println("⚠️ Event to update not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除事件 / Delete event
    public static void deleteEvent(long id) {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Event deleted successfully, ID = " + id);
            } else {
                System.out.println("⚠️ Event with ID = " + id + " not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}