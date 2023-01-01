package com.klausapp.models;

import java.time.LocalDate;

public class Rating {
    private int ticketId;
    private String category;
    private int rating;
    private float weight;
    private LocalDate createdAt;

    public int getTicketId() {
        return ticketId;
    }

    public Rating setTicketId(int ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public Rating setCategory(String category) {
        this.category = category;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public Rating setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public float getWeight() {
        return weight;
    }

    public Rating setWeight(float weight) {
        this.weight = weight;
        return this;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public Rating setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
