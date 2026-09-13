package com.mini.dns.api.dto;

import java.util.List;

import com.mini.dns.api.entity.DnsRecordType;

public class DnsResolutionResponse {
    private String hostname;
    private List<String> resolvedIps;
    private DnsRecordType recordType;
    private String pointsTo;

    public DnsResolutionResponse(
        String hostname,
        List<String> resolvedIps,
        DnsRecordType recordType,
        String pointsTo
    ){
        this.hostname = hostname;
        this.resolvedIps = resolvedIps;
        this.recordType = recordType;
        this.pointsTo = pointsTo;

    }

     public String getHostname() {
        return hostname;
    }

    public List<String> getResolvedIps() {
        return resolvedIps;
    }

    public DnsRecordType getRecordType() {
        return recordType;
    }

    public String getPointsTo() {
        return pointsTo;
    }
}
