package com.mini.dns.api.dto;

import java.util.List;

public class DnsRecordsResponse {

    private String hostname;
    private List<DnsRecordResponse> records;
    
    public DnsRecordsResponse( 
        String hostname,
        List<DnsRecordResponse> records){
            this.hostname = hostname;
            this.records = records;
    }

    public String getHostname() {
        return hostname;
    }

    public List<DnsRecordResponse> getRecords() {
        return records;
    }
    
}
