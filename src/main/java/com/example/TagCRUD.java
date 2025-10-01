package com.example;

import java.sql.*;

public class TagCRUD {
    static final String URL = "jdbc:mysql://localhost:3306/club_demo?useSSL=false&serverTimezone=UTC";
    static final String USER = "root";
    static final String PASSWORD = "123456"; // 改成你的密码 / Change to your password

    // 新增标签 / Create tag
    public static void createTag(String name, String description) {
        String sql = "INSERT INTO tags (name, description) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Tag inserted successfully, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询所有标签 / Query all tags
    public static void readTags() {
        String sql = "SELECT id, name, description FROM tags";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("📋 Tags list:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " +
                        rs.getString("name") + " | " +
                        rs.getString("description"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新标签描述 / Update tag description
    public static void updateTagDescription(int id, String newDescription) {
        String sql = "UPDATE tags SET description = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newDescription);
            stmt.setInt(2, id);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Tag updated successfully, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除标签 / Delete tag
    public static void deleteTag(int id) {
        String sql = "DELETE FROM tags WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Tag deleted successfully, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}