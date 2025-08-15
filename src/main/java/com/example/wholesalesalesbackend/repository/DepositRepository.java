package com.example.wholesalesalesbackend.repository;

import com.example.wholesalesalesbackend.model.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
        List<Deposit> findByClientId(Long clientId);

        @Query(value = "SELECT t.* FROM public.deposits t " +
                        "WHERE t.client_id = :clientId AND DATE(t.deposit_date) BETWEEN :fromDate AND :toDate " +
                        "ORDER BY t.deposit_date", nativeQuery = true)
        List<Deposit> findByClientIdAndDepositDateBetweenOrderByDepositDateDescCustom(
                        Long clientId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT t.* FROM public.deposits t " +
                        "WHERE DATE(t.deposit_date) BETWEEN :fromDate AND :toDate " +
                        "ORDER BY t.deposit_date", nativeQuery = true)
        List<Deposit> findByDepositDateBetweenOrderByDepositDateDescCustom(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT SUM(t.amount) FROM deposits t WHERE DATE(t.deposit_date) < :fromDate", nativeQuery = true)
        Double getTotalDepositOfClient(@Param("fromDate") LocalDateTime fromDate);

        @Query(value = "SELECT SUM(t.amount) FROM deposits t WHERE t.client_id = :clientId AND DATE(t.deposit_date) < :fromDate", nativeQuery = true)
        Double getTotalDepositOfClient(@Param("clientId") Long clientId,
                        @Param("fromDate") LocalDateTime fromDate);
}
