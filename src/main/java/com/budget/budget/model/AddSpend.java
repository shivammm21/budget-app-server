package com.budget.budget.model;

import java.util.List;

public class AddSpend {

    private String spendAmt;
    private String place;
    private String category;
    private String budgetId;
    //private String splitUsername;

    //public String getSplitUsername() {
    //    return splitUsername;
   // }

   // public void setSplitUsername(String splitUsername) {
    //    this.splitUsername = splitUsername;
   // }

    private List<Participant> participants;

    public String getUsername() {
        return budgetId;
    }

    public void setUsername(String username) {
        this.budgetId = username;
    }

    public String getSpendAmt() {
        return spendAmt;
    }

    public void setSpendAmt(String spendAmt) {
        this.spendAmt = spendAmt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    // Method to split the amount among friends
    public void splitAmount() {
        if (participants == null || participants.isEmpty()) {
            System.out.println("No participants to split the amount.");
            return;
        }

        try {
            double totalAmount = Double.parseDouble(spendAmt);
            double splitAmount = totalAmount / (participants.size() + 1); // Split among the user and friends

            for (Participant participant : participants) {
                participant.addToOwedAmount(splitAmount);
                // Update each participant's record in the database here (pseudo code)
                // friendsDatabase.update(participant.getUsername(), participant);
            }

            System.out.println("Amount successfully split among friends.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid spend amount.");
        }
    }
}
