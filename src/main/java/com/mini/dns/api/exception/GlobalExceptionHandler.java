package com.mini.dns.api.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidDnsRecordException.class)
    public ResponseEntity<Map<String,Object>> handleInvalidRecord(
        InvalidDnsRecordException e){
            return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
            );
        }
    

    @ExceptionHandler(DnsConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
        DnsConflictException e){
            return buildErrorResponse(
                HttpStatus.CONFLICT,
                e.getMessage()
            );
        }
    

    @ExceptionHandler(DnsRecordNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            DnsRecordNotFoundException e) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                e.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(
        MethodArgumentNotValidException e){
            String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

                return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    message
                );
        }
    
        private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String message){
                
                Map<String, Object> error = new HashMap<>();

                error.put("timestamp", LocalDateTime.now());
                error.put("status", status.value());
                error.put("error", status.getReasonPhrase());
                error.put("message", message);

                return ResponseEntity
                        .status(status)
                        .body(error);
            }   
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(
            HttpMessageNotReadableException ex) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request"
        );
    }
        
}
