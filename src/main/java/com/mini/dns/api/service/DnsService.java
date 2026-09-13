package com.mini.dns.api.service;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.mini.dns.api.dto.CreateDnsRecordRequest;
import com.mini.dns.api.dto.DnsRecordResponse;
import com.mini.dns.api.dto.DnsRecordsResponse;
import com.mini.dns.api.dto.DnsResolutionResponse;
import com.mini.dns.api.entity.DnsRecord;
import com.mini.dns.api.entity.DnsRecordType;
import com.mini.dns.api.exception.DnsConflictException;
import com.mini.dns.api.exception.DnsRecordNotFoundException;
import com.mini.dns.api.exception.InvalidDnsRecordException;
import com.mini.dns.api.repository.DnsRecordRepository;
import com.mini.dns.api.validation.DnsValidator;

@Service 
public class DnsService {

    
    private final DnsRecordRepository repository;
    private final DnsValidator validator;

    public DnsService(
            DnsRecordRepository repository,
            DnsValidator validator){
                this.repository = repository;
                this.validator = validator;
                
    }

    public DnsRecordResponse createRecord(
        CreateDnsRecordRequest request){
            String hostname = normalizeHostname(request.getHostname());
            String value = request.getValue().trim();

            validateHostname(hostname);

            if(request.getType() == DnsRecordType.A){
                validateARecord(hostname, value);
            }

            if(request.getType() == DnsRecordType.CNAME){
                value = normalizeHostname(value);
                validateCnameRecord(hostname, value);
            }

            DnsRecord record = new DnsRecord(
                hostname,
                request.getType(),
                value,
                request.getTtl()
            );

            DnsRecord saved = repository.save(record);

            return new DnsRecordResponse(
                saved.getHostname(),
                saved.getType(),
                saved.getValue(),
                saved.getCreatedAt(),
                saved.getTtl()
            );
    }

    private void validateHostname(String hostname){
        if(!validator.isValidHostname(hostname)){
            throw new InvalidDnsRecordException(
                "Invalid hostname: " + hostname
            );
        }
    }

    private void validateARecord(String hostname, String ip) {

        if (!validator.isValidIpv4(ip)) {
            throw new InvalidDnsRecordException(
                    "Invalid IPv4 address: " + ip
            );
        }

        List<DnsRecord> activeRecords = getActiveRecords(hostname);

        boolean hasCname = activeRecords.stream()
                .anyMatch(record ->
                        record.getType() == DnsRecordType.CNAME
                );

        if (hasCname) {
            throw new DnsConflictException(
                    "Cannot add A record because hostname already has a CNAME record"
            );
        }

        boolean duplicateA = activeRecords.stream()
                .anyMatch(record ->
                        record.getType() == DnsRecordType.A &&
                        record.getValue().equalsIgnoreCase(ip)
                );

        if (duplicateA) {
            throw new DnsConflictException(
                    "Duplicate A record already exists"
            );
        }
    }

    private void validateCnameRecord(
            String hostname,
            String targetHostname) {

        validateHostname(targetHostname);

        if (hostname.equalsIgnoreCase(targetHostname)) {
            throw new DnsConflictException(
                    "CNAME cannot point to itself"
            );
        }

        List<DnsRecord> activeRecords = getActiveRecords(hostname);

        boolean hasARecord = activeRecords.stream()
                .anyMatch(record ->
                        record.getType() == DnsRecordType.A
                );

        if (hasARecord) {
            throw new DnsConflictException(
                    "Cannot add CNAME because hostname already has A records"
            );
        }

        boolean hasCname = activeRecords.stream()
                .anyMatch(record ->
                        record.getType() == DnsRecordType.CNAME
                );

        if (hasCname) {
            throw new DnsConflictException(
                    "Hostname can only have one CNAME record"
            );
        }

        validateNoCircularCname(
            hostname, targetHostname);
    }

        private String normalizeHostname(String hostname){
            return hostname
                    .trim()
                    .toLowerCase()  
                    .replaceAll("\\.$", "");
        }

        
        public DnsResolutionResponse resolveHostname(String hostname){

            String normalizedHostname = normalizeHostname(hostname);

            validateHostname(normalizedHostname);

            return resolveHostnameInternal(
                normalizedHostname,
                normalizedHostname,
                new HashSet<>()
            );
        }

        private DnsResolutionResponse resolveHostnameInternal(
            String originalHostname,
            String currentHostname,
            Set<String> visited) {

        if (!visited.add(currentHostname)) {
            throw new DnsConflictException(
                    "Circular CNAME reference detected"
            );
        }

        
        List<DnsRecord> records =
                getActiveRecords(currentHostname);

        if (records.isEmpty()) {
            throw new DnsRecordNotFoundException(
                    "DNS record not found for hostname: " + currentHostname
            );
        }

        List<String> ipAddresses = records.stream()
                .filter(record -> record.getType() == DnsRecordType.A)
                .map(DnsRecord::getValue)
                .toList();

        if (!ipAddresses.isEmpty()) {

            DnsRecordType originalType =
                    originalHostname.equals(currentHostname)
                            ? DnsRecordType.A
                            : DnsRecordType.CNAME;

            return new DnsResolutionResponse(
                    originalHostname,
                    ipAddresses,
                    originalType,
                    originalHostname.equals(currentHostname)
                            ? null
                            : currentHostname
            );
        }

        DnsRecord cname = records.stream()
                .filter(record -> record.getType() == DnsRecordType.CNAME)
                .findFirst()
                .orElseThrow(() ->
                        new DnsRecordNotFoundException(
                                "No resolvable DNS record found for hostname: "
                                        + currentHostname
                        )
                );

        return resolveHostnameInternal(
                originalHostname,
                normalizeHostname(cname.getValue()),
                visited
        );
    }

    public DnsRecordsResponse getRecords(String hostname){
        String normalizedHostname = normalizeHostname(hostname);
        validateHostname(normalizedHostname);

        List<DnsRecord> records =
            getActiveRecords(normalizedHostname);

        if(records.isEmpty()){
            throw new DnsRecordNotFoundException(
                "DNS record not found for hostname: " + hostname
            );
        }

        List<DnsRecordResponse> responses = records.stream()
                .map(record -> new DnsRecordResponse(
                    record.getHostname(), 
                    record.getType(),
                    record.getValue(),
                    record.getCreatedAt(),
                    record.getTtl()
                ))
                .toList();
        return new DnsRecordsResponse(
            normalizedHostname, 
            responses);

    }

    public void deleteRecord(
            String hostname,
            DnsRecordType type,
            String value){
        String normalizedHostname = normalizeHostname(hostname);
        validateHostname(normalizedHostname);
        String normalizedValue = value.trim();

        if(type == DnsRecordType.CNAME){
            normalizedValue = normalizeHostname(normalizedValue);  
        }

        DnsRecord record = repository.findByHostnameIgnoreCaseAndTypeAndValueIgnoreCase(
            normalizedHostname, 
            type, 
            normalizedValue
            )
            .orElseThrow(
                () -> 
                    new DnsRecordNotFoundException(
                        "DNS record not found"
                    )
            );

        repository.delete(record);


    }

    private  List<DnsRecord> getActiveRecords(String hostname){
        LocalDateTime now = LocalDateTime.now();
        return repository.findByHostnameIgnoreCase(hostname)
                .stream()
                .filter(record -> 
                        record.getExpiresAt() == null ||
                        record.getExpiresAt().isAfter(now)
                )
                .toList();
    }

    private void validateNoCircularCname(
                String hostname,
                String targetHostname){
            Set<String> visited = new HashSet<>();

            String current = targetHostname;

            while (true){
                if(current.equalsIgnoreCase(hostname)){
                    throw new DnsConflictException(
                        "Circular CNAME reference detected"
                    );
                }

                if(!visited.add(current)){
                    throw new DnsConflictException(
                        "CIrcular CNAME reference detected"
                    );
                }

                List<DnsRecord> records = getActiveRecords(current);

                DnsRecord cname = records.stream()
                            .filter(record ->
                                    record.getType() == DnsRecordType.CNAME        
                            )
                            .findFirst()
                            .orElse(null);
                
                            if(cname == null){
                                break;
                            }
                            current = normalizeHostname(cname.getValue());
                        
            }
    }
}