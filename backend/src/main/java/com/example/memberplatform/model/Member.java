package com.example.memberplatform.model;

public record Member(long id, String username, String name, String passwordHash) {
}
