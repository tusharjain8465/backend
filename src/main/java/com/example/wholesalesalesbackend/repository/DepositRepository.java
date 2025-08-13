package com.example.wholesalesalesbackend.repository;

import com.example.wholesalesalesbackend.model.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
    List<Deposit> findByClientId(Long clientId);
}
