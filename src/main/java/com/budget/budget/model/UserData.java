package com.budget.budget.model;


import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "user_details")
public class UserData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String budgetId;
    String name;
    String email;
    String password;
    String spend;
    boolean showIncome = true; // Default to showing income
    Double monthlyIncome = null; // Now nullable, no default

    public Double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public boolean isShowIncome() {
        return showIncome;
    }

    public void setShowIncome(boolean showIncome) {
        this.showIncome = showIncome;
    }

    public String getSpend() {
        return spend;
    }

    public String getBudgetId() { return budgetId; }

    public void setBudgetId(String budgetId) { this.budgetId = budgetId; }

    public void setSpend(String spend) {
        this.spend = spend;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "id=" + id +
                "budgetId='" + budgetId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserData userData = (UserData) o;
        return Objects.equals(budgetId, userData.budgetId) && Objects.equals(id, userData.id) && Objects.equals(name, userData.name) && Objects.equals(email, userData.email) && Objects.equals(password, userData.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, budgetId, name, email, password);
    }
}
