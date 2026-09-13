package com.mini.dns.api.dto;


import com.mini.dns.api.entity.DnsRecordType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateDnsRecordRequest {

    @NotNull(message = "Record type is required")
    private DnsRecordType type;

    @NotBlank(message = "Hostname is required")
    private String hostname;

    @NotBlank(message = "Value is required")
    private String value;

    @Positive(message = "TTL must be greater than zero")
    private Integer ttl;

    public DnsRecordType getType() {
        return type;
    }

    public void setType(DnsRecordType type) {
        this.type = type;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }
}