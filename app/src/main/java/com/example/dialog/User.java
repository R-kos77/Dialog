package com.example.dialog;

public class User {
    public String firstName;
    public String lastName;
    public int age;
    public String gender;
    public String id;

    public User() {
        // Required empty constructor
    }

    public User(String firstName, String lastName, int age, String gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.gender = gender;
    }

    public User(String id, String firstName, String lastName, int age, String gender) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.gender = gender;
    }
} 