package com.example.wholesalesalesbackend.service;

import com.example.wholesalesalesbackend.dto.DepositUpdateRequest;
import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.repository.DepositRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepositService {

    @Autowired
    private DepositRepository depositRepository;

    public Deposit addDeposit(Deposit deposit) {
        String addDepositPrefix = "DEPOSIT -> "+ deposit.getNote();
        deposit.setNote(addDepositPrefix);
        return depositRepository.save(deposit);
    }

    public List<Deposit> getDepositsByClientId() {
        return depositRepository.findAll();
    }

    public Deposit updateDeposit(Long id, DepositUpdateRequest request) {
        Deposit deposit = depositRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        deposit.setAmount(request.getAmount());
        deposit.setNote(request.getNote());
        return depositRepository.save(deposit);
    }

    public void deleteDeposit(Long id) {
        depositRepository.deleteById(id);
    }
}
