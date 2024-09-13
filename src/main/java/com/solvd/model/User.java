package com.solvd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    private long id;
    private String name;
    private String email;
    private String gender;
    private String status;

    public User() {
    }

    public User(String name, String email, String gender, String status) {
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.status = status;
    }

    @JsonIgnore
    public long getId() {
        return id;
    }

    @JsonProperty
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public boolean isValid() {
        return (id > 0) &&
                (name != null && !name.isEmpty()) &&
                (email != null && !email.isEmpty()) &&
                (gender != null && !gender.isEmpty()) &&
                (status != null && !status.isEmpty());
    }

    public boolean dataEquals(User user) {
        return name.equals(user.name) &&
                email.equals(user.email) &&
                gender.equals(user.gender) &&
                status.equals(user.status);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", gender='" + gender + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
