package com.mini.dns.api.repository;

import com.mini.dns.api.entity.DnsRecord;
import com.mini.dns.api.entity.DnsRecordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DnsRecordRepository extends JpaRepository<DnsRecord, Long> {

    List<DnsRecord> findByHostnameIgnoreCase(String hostname);

    Optional<DnsRecord> findByHostnameIgnoreCaseAndTypeAndValueIgnoreCase(
            String hostname,
            DnsRecordType type,
            String value
    );

    boolean existsByHostnameIgnoreCaseAndType(
            String hostname,
            DnsRecordType type
    );

    boolean existsByHostnameIgnoreCaseAndTypeAndValueIgnoreCase(
            String hostname,
            DnsRecordType type,
            String value
    );

    void deleteByExpiresAtBefore(LocalDateTime time);
}