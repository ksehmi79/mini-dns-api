package com.mini.dns.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dns_records")
public class DnsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hostname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DnsRecordType type;

    @Column(name = "record_value", nullable = false)
    private String value;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Integer ttl;

    private LocalDateTime expiresAt;

    public DnsRecord() {
    }

    public DnsRecord(
            String hostname,
            DnsRecordType type,
            String value,
            Integer ttl) {

        this.hostname = hostname;
        this.type = type;
        this.value = value;
        this.ttl = ttl;
        this.createdAt = LocalDateTime.now();

        if (ttl != null) {
            this.expiresAt = this.createdAt.plusSeconds(ttl);
        }
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (ttl != null && expiresAt == null) {
            expiresAt = createdAt.plusSeconds(ttl);
        }
    }

    public Long getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public DnsRecordType getType() {
        return type;
    }

    public void setType(DnsRecordType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}