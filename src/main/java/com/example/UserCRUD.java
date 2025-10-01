package com.example;

import java.sql.*;

public class UserCRUD {
    // 数据库连接信息 / Database connection information
    static final String URL = "jdbc:mysql://localhost:3306/club_demo?useSSL=false&serverTimezone=UTC";
    static final String USER = "root";
    static final String PASSWORD = "123456"; // 改成你自己的密码 / Change to your own password

    // 新增用户 / Create user
    public static void createUser(String username, String fullName, String role, String status) {
        String sql = "INSERT INTO users (username, full_name, role, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, role);
            stmt.setString(4, status);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Insert successful, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询所有用户 / Query all users
    public static void readUsers() {
        String sql = "SELECT id, username, full_name, role, status FROM users";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("📋 Users list:");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("id") + " | " +
                                rs.getString("username") + " | " +
                                rs.getString("full_name") + " | " +
                                rs.getString("role") + " | " +
                                rs.getString("status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新用户 / Update user
    public static void updateUserRole(int id, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setInt(2, id);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Update successful, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除用户 / Delete user
    public static void deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Delete successful, affected rows: " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}