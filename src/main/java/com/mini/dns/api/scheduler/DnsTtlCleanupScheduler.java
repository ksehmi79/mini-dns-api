package com.mini.dns.api.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mini.dns.api.repository.DnsRecordRepository;

import jakarta.transaction.Transactional;

@Component 
public class DnsTtlCleanupScheduler {
    private final DnsRecordRepository repository;

    public DnsTtlCleanupScheduler(
        DnsRecordRepository repository){
            this.repository = repository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional 
    public void removeExpiredRecords(){
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }


}
