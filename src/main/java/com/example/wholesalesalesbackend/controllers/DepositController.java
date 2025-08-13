package com.example.wholesalesalesbackend.controllers;

import com.example.wholesalesalesbackend.dto.DepositUpdateRequest;
import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.service.DepositService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposits")
public class DepositController {

    @Autowired(required = false)
    private DepositService depositService;

    // Add a deposit
    @PostMapping("/add")
    public ResponseEntity<Deposit> addDeposit(@RequestBody Deposit deposit) {
        Deposit saved = depositService.addDeposit(deposit);
        return ResponseEntity.ok(saved);
    }

    // Get all deposits for a client
    @GetMapping("/all")
    public ResponseEntity<List<Deposit>> getDepositsByClient() {
        List<Deposit> deposits = depositService.getDepositsByClientId();
        return ResponseEntity.ok(deposits);
    }

        @PutMapping("/update/{id}")
    public ResponseEntity<Deposit> updateDeposit(
            @PathVariable Long id,
            @RequestBody DepositUpdateRequest request) {
        Deposit updated = depositService.updateDeposit(id, request);
        return ResponseEntity.ok(updated);
    }

    // -------- Delete deposit --------
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteDeposit(@PathVariable Long id) {
        depositService.deleteDeposit(id);
        return ResponseEntity.noContent().build();
    }

}
