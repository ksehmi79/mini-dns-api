package com.mini.dns.api.exception;

public class InvalidDnsRecordException extends RuntimeException     {
    public InvalidDnsRecordException(String message){
        super(message);
    }    
}
