package com.example.wholesalesalesbackend.dto;

public class DepositUpdateRequest {
    private Double amount;
    private String note;

    // getters and setters
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
