package com.example.wholesalesalesbackend.controllers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.GraphResponseDTO;
import com.example.wholesalesalesbackend.dto.ProfitAndSale;
import com.example.wholesalesalesbackend.dto.SaleAttributeUpdateDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryRequestDTO;
import com.example.wholesalesalesbackend.dto.SaleUpdateRequest;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;
import com.example.wholesalesalesbackend.service.SaleEntryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
public class SaleEntryController {

    @Autowired(required = false)
    private SaleEntryService saleEntryService;

    @Autowired(required = false)
    SaleEntryRepository saleEntryRepository;

    @PostMapping("/sale-entry/add")
    public ResponseEntity<String> addSaleEntry(@RequestBody SaleEntryRequestDTO requestDTO) {
        SaleEntry savedEntry = saleEntryService.addSaleEntry(requestDTO);
        return ResponseEntity.ok("added");
    }

    @GetMapping("/all-sales/all")
    public ResponseEntity<List<SaleEntryDTO>> getAllSales() {
        List<SaleEntry> entries = saleEntryService.getAllSales();

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

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all-sales-new")
    public ResponseEntity<Page<SaleEntryDTO>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String searchText) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("saleDateTime").descending());

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        Page<SaleEntry> entries = saleEntryRepository.findAllWithFilters(
                clientId, startDateTime, endDateTime, searchText, pageable);

        Page<SaleEntryDTO> dtos = entries.map(this::toDTO);
        return ResponseEntity.ok(dtos);
    }

    private SaleEntryDTO toDTO(SaleEntry entry) {
        SaleEntryDTO dto = new SaleEntryDTO();
        dto.setId(entry.getId());
        dto.setAccessoryName(entry.getAccessoryName());
        dto.setQuantity(entry.getQuantity());
        dto.setTotalPrice(entry.getTotalPrice());
        dto.setProfit(entry.getProfit());
        dto.setReturnFlag(entry.isReturnFlag());
        dto.setSaleDateTime(entry.getSaleDateTime());

        if (entry.getClient() != null) {
            dto.setClientName(entry.getClient().getName());
        }

        return dto;
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<SaleEntry>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        return ResponseEntity.ok(saleEntryService.getSalesByDateRange(from, to));
    }

    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<SaleEntryDTO>> getSalesByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(saleEntryService.getSalesEntryDTOByClient(clientId));
    }

    @PutMapping("/by-client/{clientId}")
    public ResponseEntity<String> updateSalesByClient(
            @PathVariable Long clientId, @RequestParam(value = "saleEntryId", required = true) Long saleEntryId,
            @RequestBody SaleUpdateRequest request) {
        saleEntryService.updateSalesByClient(clientId, saleEntryId, request);
        return ResponseEntity.ok("Updated !!!");
    }

    @GetMapping("/by-client-and-date-range")
    public ResponseEntity<List<SaleEntryDTO>> getSalesByClientAndDateRange(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {

        List<SaleEntryDTO> sales = saleEntryService.getSalesEntryDTOByClientAndDateRange(clientId, from, to);
        return ResponseEntity.ok(sales);
    }

    @PutMapping("/sale-entry/few-attributes")
    public ResponseEntity<String> updateProfit(@RequestBody SaleAttributeUpdateDTO dto) {
        SaleEntry updated = saleEntryService.updateProfit(dto);
        return ResponseEntity.ok("updated!!!");
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<SaleEntryDTO> updateSaleEntry(@PathVariable Long id,
            @RequestBody @Valid SaleEntryDTO requestDTO) {
        SaleEntryDTO updated = saleEntryService.updateSaleEntry(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteSaleEntry(@PathVariable Long id) {
        String output = saleEntryService.deleteSaleEntry(id);
        return ResponseEntity.ok(output);
    }

    @GetMapping("/profit/by-date-range")
    public ResponseEntity<ProfitAndSale> getProfitByDateRange(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(required = false) Long days,
            @RequestParam(required = false) Long clientId) {

        return ResponseEntity.ok(saleEntryService.getTotalProfitByDateRange(from, to, days, clientId));
    }

    @GetMapping("/graph-data")
    public GraphResponseDTO getSalesData(@RequestParam String period) {
        GraphResponseDTO response = new GraphResponseDTO();
        LocalDate today = LocalDate.now();

        List<String> labels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();
        List<Double> profitData = new ArrayList<>();

        List<SaleEntry> entries;

        switch (period.toLowerCase()) {
            case "today": {
                YearMonth currentMonth = YearMonth.from(today);
                LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
                LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

                entries = saleEntryRepository.findBySaleDateTimeBetween(monthStart, monthEnd);

                // Group entries by day
                Map<LocalDate, List<SaleEntry>> entriesByDay = entries.stream()
                        .collect(Collectors.groupingBy(e -> e.getSaleDateTime().toLocalDate()));

                int daysInMonth = today.lengthOfMonth();
                labels = IntStream.rangeClosed(1, daysInMonth)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.toList());

                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate date = LocalDate.of(today.getYear(), today.getMonth(), day);
                    List<SaleEntry> dailyEntries = entriesByDay.getOrDefault(date, Collections.emptyList());

                    Double dailySale = dailyEntries.stream()
                            .mapToDouble(e -> e.getTotalPrice() != null ? e.getTotalPrice() : 0.0)
                            .sum();

                    Double dailyProfit = dailyEntries.stream()
                            .mapToDouble(e -> e.getProfit() != null ? e.getProfit() : 0.0)
                            .sum();

                    salesData.add(dailySale);
                    profitData.add(dailyProfit);
                }

                break;
            }

            case "week": {
                LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
                LocalDateTime weekStart = startOfWeek.atStartOfDay();
                LocalDateTime weekEnd = startOfWeek.plusDays(6).atTime(23, 59, 59);

                entries = saleEntryRepository.findBySaleDateTimeBetween(weekStart, weekEnd);

                Map<LocalDate, List<SaleEntry>> entriesByDay = entries.stream()
                        .collect(Collectors.groupingBy(e -> e.getSaleDateTime().toLocalDate()));

                labels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

                for (int i = 0; i < 7; i++) {
                    LocalDate date = startOfWeek.plusDays(i);
                    List<SaleEntry> dailyEntries = entriesByDay.getOrDefault(date, Collections.emptyList());

                    Double dailySale = dailyEntries.stream()
                            .mapToDouble(e -> e.getTotalPrice() != null ? e.getTotalPrice() : 0.0)
                            .sum();

                    Double dailyProfit = dailyEntries.stream()
                            .mapToDouble(e -> e.getProfit() != null ? e.getProfit() : 0.0)
                            .sum();

                    salesData.add(dailySale);
                    profitData.add(dailyProfit);
                }

                break;
            }

            case "month": {
                LocalDateTime yearStart = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
                LocalDateTime yearEnd = LocalDate.of(today.getYear(), 12, 31).atTime(23, 59, 59);

                entries = saleEntryRepository.findBySaleDateTimeBetween(yearStart, yearEnd);

                // Group entries by month
                Map<Integer, List<SaleEntry>> entriesByMonth = entries.stream()
                        .collect(Collectors.groupingBy(e -> e.getSaleDateTime().getMonthValue()));

                labels = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

                for (int month = 1; month <= 12; month++) {
                    List<SaleEntry> monthlyEntries = entriesByMonth.getOrDefault(month, Collections.emptyList());

                    Double monthlySale = monthlyEntries.stream()
                            .mapToDouble(e -> e.getTotalPrice() != null ? e.getTotalPrice() : 0.0)
                            .sum();

                    Double monthlyProfit = monthlyEntries.stream()
                            .mapToDouble(e -> e.getProfit() != null ? e.getProfit() : 0.0)
                            .sum();

                    salesData.add(monthlySale);
                    profitData.add(monthlyProfit);
                }

                break;
            }

            default:
                throw new IllegalArgumentException("Invalid period. Use today, week, or month.");
        }

        double averageSale = salesData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double averageProfit = profitData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double highestSale = salesData.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double highestProfit = profitData.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        response.setLabels(labels);
        response.setSalesData(salesData);
        response.setProfitData(profitData);
        response.setAverageSale(Math.round(averageSale * 100.0) / 100.0);
        response.setAverageProfit(Math.round(averageProfit * 100.0) / 100.0);
        response.setHighestSale(Math.round(highestSale * 100.0) / 100.0);
        response.setHighestProfit(Math.round(highestProfit * 100.0) / 100.0);

        return response;
    }

}
