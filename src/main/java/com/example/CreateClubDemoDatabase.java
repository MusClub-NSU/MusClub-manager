package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateClubDemoDatabase {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "123456";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 2. Get connected
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();

            // 3. Execute the create database statement
            String createDB = "CREATE DATABASE IF NOT EXISTS club_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
            stmt.executeUpdate(createDB);
            System.out.println("The database club_demo was created successfully");
            // 4. Switch to database
            stmt.executeUpdate("USE club_demo;");

            // 5. Execute the table-building statement
            String sql = """
                CREATE TABLE IF NOT EXISTS tags (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  name VARCHAR(100) NOT NULL UNIQUE,
                  description VARCHAR(255),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                CREATE TABLE IF NOT EXISTS users (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  username VARCHAR(100) NOT NULL UNIQUE,
                  full_name VARCHAR(200),
                  social_links JSON NULL,
                  role VARCHAR(50),
                  status VARCHAR(50),
                  specialization VARCHAR(200),
                  tools VARCHAR(255),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                CREATE TABLE IF NOT EXISTS events (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  title VARCHAR(255) NOT NULL,
                  description TEXT,
                  event_time DATETIME,
                  location VARCHAR(255),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                CREATE TABLE IF NOT EXISTS user_tags (
                  user_id BIGINT NOT NULL,
                  tag_id BIGINT NOT NULL,
                  PRIMARY KEY (user_id, tag_id),
                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                CREATE TABLE IF NOT EXISTS event_tags (
                  event_id BIGINT NOT NULL,
                  tag_id BIGINT NOT NULL,
                  PRIMARY KEY (event_id, tag_id),
                  FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;

            for (String s : sql.split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.executeUpdate(trimmed);
                }
            }

            System.out.println("All tables were created successfully!");

            // 6. 关闭连接
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
