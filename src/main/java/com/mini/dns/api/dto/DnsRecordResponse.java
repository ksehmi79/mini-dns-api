package com.mini.dns.api.dto;

import com.mini.dns.api.entity.DnsRecordType;

import java.time.LocalDateTime;

public class DnsRecordResponse {

    private String hostname;
    private DnsRecordType type;
    private String value;
    private LocalDateTime createdAt;
    private Integer ttl;

    public DnsRecordResponse(
            String hostname,
            DnsRecordType type,
            String value,
            LocalDateTime createdAt,
            Integer ttl) {

        this.hostname = hostname;
        this.type = type;
        this.value = value;
        this.createdAt = createdAt;
        this.ttl = ttl;
    }

    public String getHostname() {
        return hostname;
    }

    public DnsRecordType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getTtl() {
        return ttl;
    }
}