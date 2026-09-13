package com.mini.dns.api.exception;

public class DnsRecordNotFoundException extends RuntimeException {
    public DnsRecordNotFoundException(String message){
        super(message);
    }
    
}
