package com.example;

public class UserTest {
    public static void main(String[] args) {
        // 1. 新增用户 / Create users
        UserCRUD.createUser("alice", "Alice Chen", "admin", "active");
        UserCRUD.createUser("bob", "Bob Lee", "member", "inactive");

        // 2. 查询 / Query
        UserCRUD.readUsers();

        // 3. 更新 / Update
        UserCRUD.updateUserRole(1, "superadmin");

        // 4. 删除 / Delete
        UserCRUD.deleteUser(2);

        // 再次查询 / Query again
        UserCRUD.readUsers();
    }
}