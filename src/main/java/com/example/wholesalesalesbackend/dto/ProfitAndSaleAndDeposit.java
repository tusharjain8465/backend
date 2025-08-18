package com.example.wholesalesalesbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitAndSaleAndDeposit {

    private Double totalSales;
    private Double actualSales;
    private Double profit;
    private Double deposit;
}

