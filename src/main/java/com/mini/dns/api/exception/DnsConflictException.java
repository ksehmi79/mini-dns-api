package com.mini.dns.api.exception;

public class DnsConflictException extends RuntimeException {
    public DnsConflictException(String message){
        super(message);
    }
}
