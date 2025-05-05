package com.budget.budget.model;

public class Participant {

    private String username;
    private double owedAmount;

    public Participant(String username) {
        this.username = username;
        this.owedAmount = 0.0;
    }

    public String getUsername() {
        return username;
    }

    public double getOwedAmount() {
        return owedAmount;
    }

    public void addToOwedAmount(double amount) {
        this.owedAmount += amount;
    }
}
