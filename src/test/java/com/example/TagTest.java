package com.example;

public class TagTest {
    public static void main(String[] args) {
        TagCRUD.createTag("Java", "Java development tag");
        TagCRUD.createTag("MySQL", "Database related");

        TagCRUD.readTags();

        TagCRUD.updateTagDescription(1, "Content related to Java development");

        TagCRUD.deleteTag(2);

        TagCRUD.readTags();
    }
}