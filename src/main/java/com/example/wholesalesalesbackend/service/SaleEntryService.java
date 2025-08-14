package com.example.wholesalesalesbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.ProfitAndSale;
import com.example.wholesalesalesbackend.dto.SaleAttributeUpdateDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryRequestDTO;
import com.example.wholesalesalesbackend.dto.SaleUpdateRequest;
import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.repository.ClientRepository;
import com.example.wholesalesalesbackend.repository.ProfitAndSaleProjection;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SaleEntryService {

    @Autowired
    private SaleEntryRepository saleEntryRepository;

    @Autowired
    private ClientRepository clientRepository;

    public SaleEntry addSaleEntry(SaleEntryRequestDTO dto) {

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        boolean isReturn = Boolean.TRUE.equals(dto.getReturnFlag());

        String accessoryName = Optional.ofNullable(dto.getAccessoryName()).orElse("UNKNOWN");
        accessoryName = isReturn ? "RETURN -> " + accessoryName : "ADD -> " + accessoryName;

        Double totalPrice = Optional.ofNullable(dto.getTotalPrice()).orElse(0.0);
        Double profit = Optional.ofNullable(dto.getProfit()).orElse(0.0);

        if (isReturn) {
            totalPrice = -Math.abs(totalPrice);
            profit = -Math.abs(profit);
        } else {
            totalPrice = Math.abs(totalPrice);
            profit = Math.abs(profit);
        }

        // Sale date in IST
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        LocalDateTime saleDateTime = Optional.ofNullable(dto.getSaleDateTime())
                .orElse(LocalDateTime.now(indiaZone));

        SaleEntry saleEntry = SaleEntry.builder()
                .accessoryName(accessoryName)
                .quantity(Optional.ofNullable(dto.getQuantity()).orElse(1))
                .totalPrice(totalPrice)
                .profit(profit)
                .saleDateTime(saleDateTime)
                .note(dto.getNote())
                .returnFlag(isReturn)
                .client(client)
                .build();

        return saleEntryRepository.save(saleEntry);
    }

    public List<SaleEntry> getSalesByClientAndDateRange(Long clientId, LocalDateTime from, LocalDateTime to) {

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        // Convert inputs to IST (only if they are not null)
        if (from != null) {
            from = from.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }
        if (to != null) {
            to = to.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }

        List<SaleEntry> entries = new ArrayList<>();

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            if (from != null && to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(client, from,
                        to);
            } else if (from != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeAfterOrderBySaleDateTimeDesc(client, from);
            } else if (to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(client, to);
            } else {
                entries = saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client);
            }
        } else {
            // All clients
            if (from != null && to != null) {
                entries = saleEntryRepository.findBySaleDateTimeBetweenOrderBySaleDateTimeDesc(from, to);
            } else if (from != null) {
                entries = saleEntryRepository.findBySaleDateTimeAfterOrderBySaleDateTimeDesc(from);
            } else if (to != null) {
                entries = saleEntryRepository.findBySaleDateTimeBeforeOrderBySaleDateTimeDesc(to);
            } else {
                entries = saleEntryRepository.findAllByOrderBySaleDateTimeDesc();
            }
        }

        return entries;
    }

    public List<SaleEntryDTO> getSalesEntryDTOByClientAndDateRange(Long clientId, LocalDateTime from,
            LocalDateTime to) {

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        // Convert incoming times to IST if they are not null
        if (from != null) {
            from = from.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }
        if (to != null) {
            to = to.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }

        List<SaleEntry> entries = new ArrayList<>();
        List<SaleEntryDTO> dtos = new ArrayList<>();

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            if (from != null && to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(client, from,
                        to);
            } else if (from != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeAfterOrderBySaleDateTimeDesc(client, from);
            } else if (to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(client, to);
            } else {
                entries = saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client);
            }

        } else {
            // For all clients
            if (from != null && to != null) {
                entries = saleEntryRepository.findBySaleDateTimeBetweenOrderBySaleDateTimeDesc(from, to);
            } else if (from != null) {
                entries = saleEntryRepository.findBySaleDateTimeAfterOrderBySaleDateTimeDesc(from);
            } else if (to != null) {
                entries = saleEntryRepository.findBySaleDateTimeBeforeOrderBySaleDateTimeDesc(to);
            } else {
                entries = saleEntryRepository.findAllByOrderBySaleDateTimeDesc();
            }
        }

        for (SaleEntry sale : entries) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.isReturnFlag());
            dto.setAccessoryName(sale.getAccessoryName());
            dto.setNote(sale.getNote());

            dtos.add(dto);

        }

        return dtos;

    }

    public SaleEntry updateProfit(SaleAttributeUpdateDTO dto) {
        SaleEntry entry = saleEntryRepository.findById(dto.getSaleEntryId())
                .orElseThrow(() -> new RuntimeException("SaleEntry not found with id: " + dto.getSaleEntryId()));

        entry.setProfit(dto.getProfit());
        entry.setAccessoryName(dto.getAccessory());
        entry.setTotalPrice(dto.getTotalPrice());

        return saleEntryRepository.save(entry);
    }

    public List<SaleEntry> getSalesEntryByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client);
    }

    public List<SaleEntryDTO> getSalesEntryDTOByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        List<SaleEntry> entries = saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client);

        List<SaleEntryDTO> dtos = new ArrayList<>();
        for (SaleEntry sale : entries) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.isReturnFlag());
            dto.setNote(sale.getNote());
            dto.setAccessoryName(sale.getAccessoryName());

            dtos.add(dto);

        }

        return dtos;
    }

    @Transactional
    public int updateSalesByClient(Long clientId, Long saleEntryId, SaleUpdateRequest saleUpdateRequest) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        String newAccessoryName = saleUpdateRequest.getAccessoryName();
        LocalDateTime now = saleUpdateRequest.getSaleDateTime();
        Double newTotalPrice = saleUpdateRequest.getTotalPrice();

        return saleEntryRepository.updateSalesByClient(newAccessoryName, now, newTotalPrice, client.getId(),
                saleEntryId);
    }

    public List<SaleEntry> getSalesByDateRange(LocalDateTime from, LocalDateTime to) {
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (from != null) {
            from = from.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }
        if (to != null) {
            to = to.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }

        return saleEntryRepository.findBySaleDateTimeBetweenOrderBySaleDateTimeDesc(from, to);
    }

    public List<SaleEntry> getAllSales() {
        return saleEntryRepository.findAllByOrderBySaleDateTimeDesc();
    }

    public SaleEntryDTO updateSaleEntry(Long id, SaleEntryDTO updatedEntry) {
        SaleEntry existing = saleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SaleEntry not found"));

        existing
                .setAccessoryName(
                        Boolean.TRUE.equals(updatedEntry.getReturnFlag())
                                ? "RETURN -> " + updatedEntry.getAccessoryName()
                                : "ADD -> " + updatedEntry.getAccessoryName());
        existing.setQuantity(updatedEntry.getQuantity());
        existing.setTotalPrice(updatedEntry.getTotalPrice());
        existing.setReturnFlag(updatedEntry.getReturnFlag());
        existing.setSaleDateTime(updatedEntry.getSaleDateTime());
        existing.setProfit(updatedEntry.getProfit());
        existing.setNote(updatedEntry.getNote());
        existing.setAccessoryName(updatedEntry.getAccessoryName());

        if (updatedEntry.getClientName() != null) {
            Client client = clientRepository.findByName(updatedEntry.getClientName())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            existing.setClient(client);
        }

        existing = saleEntryRepository.save(existing);

        SaleEntryDTO dto = new SaleEntryDTO();
        dto.setId(id);
        dto.setProfit(existing.getProfit());
        dto.setQuantity(existing.getQuantity());
        dto.setClientName(existing.getClient().getName());
        dto.setSaleDateTime(existing.getSaleDateTime());
        dto.setTotalPrice(existing.getTotalPrice());
        dto.setReturnFlag(existing.isReturnFlag());
        dto.setNote(existing.getNote());
        dto.setAccessoryName(existing.getAccessoryName());

        return dto;
    }

    public String deleteSaleEntry(Long id) {
        SaleEntry existing = saleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SaleEntry not found"));
        saleEntryRepository.delete(existing);
        return "Deleted !!!";
    }

    public ProfitAndSale getTotalProfitByDateRange(LocalDateTime from, LocalDateTime to, Long days, Long clientId) {

        // If 'days' is provided but no from/to, calculate date range
        if (days != null && from == null && to == null) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            to = today.atTime(LocalTime.MAX); // today end of day
            from = today.minusDays(days).atStartOfDay(); // days ago start of day

        }

        ProfitAndSaleProjection result;

        // If clientId is provided, use client-specific repository method
        if (clientId != null) {
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDatesByClient(from,
                    to, clientId);
        } else {
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDates(from, to);
        }

        // Handle null projection result
        if (result == null) {
            return new ProfitAndSale(0.0, 0.0);
        }

        return new ProfitAndSale(result.getSale(), result.getProfit());
    }

    public ProfitAndSale getTotalSaleDateRange(LocalDateTime from, LocalDateTime to, Long clientId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        if (from != null) {
            from = today.atStartOfDay(); // days ago start of day

        }
        if (to != null) {
            to = today.atTime(LocalTime.MAX); // today end of day
        }

        ProfitAndSaleProjection result = null;

        boolean isDateRangeProvided = (from != null && to != null);
        boolean isClientProvided = (clientId != null);

        if (isDateRangeProvided && isClientProvided) {
            // Case 1: Both dates and client ID are provided
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDatesByClient(from,
                    to, clientId);
        } else if (isDateRangeProvided) {
            // Case 2: Only dates provided
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDates(from, to);
        } else if (isClientProvided) {
            // Case 3: Only client ID provided
            result = saleEntryRepository.getTotalPriceAndProfitByClient(clientId);
        } else {
            // Case 4: No filters
            result = saleEntryRepository.getTotalPriceAndProfit();
        }

        if (result == null) {
            return new ProfitAndSale(0.0, 0.0);
        }

        return new ProfitAndSale(result.getSale(), result.getProfit());
    }

}
