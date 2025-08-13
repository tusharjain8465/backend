package com.example.wholesalesalesbackend.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;
import com.example.wholesalesalesbackend.service.ClientService;
import com.example.wholesalesalesbackend.service.PdfService;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired(required = false)
    private PdfService pdfService;

    @Autowired(required = false)
    private ClientService clientService;

    @Autowired(required = false)
    SaleEntryRepository saleEntryRepository;

    @GetMapping("/sales")
    public ResponseEntity<byte[]> generateSalesPdf(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime depositDatetime,
            @RequestParam(required = false, defaultValue = "0") Integer days,
            @RequestParam(required = false) Double oldBalance,
            @RequestParam(required = false, defaultValue = "0") Double depositAmount) throws IOException {

        ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
        boolean isAllClient = (clientId == null);
        String clientName;
        List<SaleEntry> sales = new ArrayList<>();

        // If no dates provided, use current India time
        if (to == null && from == null) {
            LocalDate today = LocalDate.now(INDIA_ZONE);
            to = today.atTime(LocalTime.MAX); // today end of day
            from = today.minusDays(days).atStartOfDay(); // days ago start of day
        }

        LocalDate fromLocalDate = from.toLocalDate();
        LocalDate toLocalDate = to.toLocalDate();

        if (!isAllClient) {
            Client client = clientService.getClientById(clientId);
            clientName = client.getName();

            if (oldBalance == null) {
                oldBalance = saleEntryRepository.getOldBalanceOfClient(clientId, from);
            }

            sales = saleEntryRepository.findByClientIdAndSaleDateBetweenOrderBySaleDateTimeDescCustom(
                    clientId, fromLocalDate, toLocalDate);

        } else {
            clientName = "All_Clients";

            if (oldBalance == null) {
                oldBalance = saleEntryRepository.getOldBalance(from);
            }

            sales = saleEntryRepository.findBySaleDateBetweenOrderBySaleDateTimeDescCustom(
                    fromLocalDate, toLocalDate);
        }

        ByteArrayInputStream bis = pdfService.generateSalesPdf(
                clientName, sales, fromLocalDate, toLocalDate, isAllClient,
                depositAmount, depositDatetime, oldBalance);

        byte[] pdfBytes = bis.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline")
                .filename("sales_report_" + clientName + ".pdf")
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/ping")
    public Map<String, Object> pingBackend() {
        Map<String, Object> response = new HashMap<>();
        System.out.println("status --> alive");
        response.put("status", "success");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

}
