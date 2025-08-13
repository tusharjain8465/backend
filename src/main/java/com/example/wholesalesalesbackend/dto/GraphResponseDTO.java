package com.example.wholesalesalesbackend.dto;

import java.util.List;

public class GraphResponseDTO {
    private List<String> labels;
    private List<Double> salesData;
    private List<Double> profitData;
    private double averageSale;
    private double averageProfit;
    private double highestSale;
    private double highestProfit;

    // Getters & Setters
    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Double> getSalesData() {
        return salesData;
    }

    public void setSalesData(List<Double> salesData) {
        this.salesData = salesData;
    }

    public List<Double> getProfitData() {
        return profitData;
    }

    public void setProfitData(List<Double> profitData) {
        this.profitData = profitData;
    }

    public double getAverageSale() {
        return averageSale;
    }

    public void setAverageSale(double averageSale) {
        this.averageSale = averageSale;
    }

    public double getAverageProfit() {
        return averageProfit;
    }

    public void setAverageProfit(double averageProfit) {
        this.averageProfit = averageProfit;
    }

    public double getHighestSale() {
        return highestSale;
    }

    public void setHighestSale(double highestSale) {
        this.highestSale = highestSale;
    }

    public double getHighestProfit() {
        return highestProfit;
    }

    public void setHighestProfit(double highestProfit) {
        this.highestProfit = highestProfit;
    }
}

