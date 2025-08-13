package com.example.wholesalesalesbackend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.ClientCreateRequest;
import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.service.ClientService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "https://frontendpoller1.vercel.app")
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired(required = false)
    private ClientService clientService;

    @PostMapping("/add")
    public ResponseEntity<Client> addClient(@RequestBody @Valid ClientCreateRequest request) {
        Client newClient = Client.builder()
                .name(request.getName())
                .location(request.getLocation())
                .contact(request.getContact())
                .build();

        return ResponseEntity.ok(clientService.addClient(newClient));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody @Valid ClientCreateRequest request) {
        Client updatedClient = clientService.updateClient(id, request);
        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable Long id) {
        String output = clientService.deleteClient(id);
        return ResponseEntity.ok(output);
    }

}
