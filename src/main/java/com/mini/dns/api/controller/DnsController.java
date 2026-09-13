package com.mini.dns.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.mini.dns.api.dto.CreateDnsRecordRequest;
import com.mini.dns.api.dto.DnsRecordResponse;
import com.mini.dns.api.dto.DnsRecordsResponse;
import com.mini.dns.api.dto.DnsResolutionResponse;
import com.mini.dns.api.entity.DnsRecordType;
import com.mini.dns.api.service.DnsService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/dns")
public class DnsController {

    private final DnsService dnsService;

    public  DnsController(DnsService dnsService){
        this.dnsService = dnsService;
    }

    @PostMapping
    public ResponseEntity<DnsRecordResponse> createRecord(
        @Valid @RequestBody CreateDnsRecordRequest request) {
        
        DnsRecordResponse response = 
            dnsService.createRecord(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    


    @GetMapping("/{hostname}")
    public  ResponseEntity<DnsResolutionResponse> resolveHostname(
        @PathVariable String hostname){

            DnsResolutionResponse response = dnsService.resolveHostname(hostname);
            return ResponseEntity.ok(response);
    }

    @GetMapping("/{hostname}/records")
    public ResponseEntity<DnsRecordsResponse> getRecords(
        @PathVariable String hostname) {
        
        DnsRecordsResponse response = dnsService.getRecords(hostname);

        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{hostname}")
    public  ResponseEntity<Void> deleteRecord(
        @PathVariable String hostname,
        @RequestParam DnsRecordType type,
        @RequestParam String value){
            dnsService.deleteRecord(
                hostname, 
                type, 
                value
            );

        return ResponseEntity.noContent().build();
    }
    

    

}
