package com.mini.dns.api.controller;

import com.mini.dns.api.dto.DnsRecordResponse;
import com.mini.dns.api.dto.DnsResolutionResponse;
import com.mini.dns.api.entity.DnsRecordType;
import com.mini.dns.api.exception.DnsConflictException;
import com.mini.dns.api.exception.DnsRecordNotFoundException;
import com.mini.dns.api.exception.GlobalExceptionHandler;
import com.mini.dns.api.service.DnsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DnsControllerTest {

    private DnsService dnsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        dnsService = mock(DnsService.class);

        DnsController controller =
                new DnsController(dnsService);

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturn201WhenCreatingRecord() throws Exception {

        DnsRecordResponse response =
                new DnsRecordResponse(
                        "example.com",
                        DnsRecordType.A,
                        "192.168.1.1",
                        LocalDateTime.now(),
                        null
                );

        when(dnsService.createRecord(any()))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/dns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "A",
                                  "hostname": "example.com",
                                  "value": "192.168.1.1"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hostname")
                .value("example.com"))
        .andExpect(jsonPath("$.type")
                .value("A"))
        .andExpect(jsonPath("$.value")
                .value("192.168.1.1"));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {

        mockMvc.perform(
                post("/api/dns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "A",
                                  "hostname": "",
                                  "value": ""
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409ForConflict() throws Exception {

        when(dnsService.createRecord(any()))
                .thenThrow(
                        new DnsConflictException(
                                "Duplicate A record already exists"
                        )
                );

        mockMvc.perform(
                post("/api/dns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "A",
                                  "hostname": "example.com",
                                  "value": "192.168.1.1"
                                }
                                """)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
                .value("Duplicate A record already exists"));
    }

    @Test
    void shouldReturn404WhenHostnameDoesNotExist()
            throws Exception {

        when(dnsService.resolveHostname("missing.com"))
                .thenThrow(
                        new DnsRecordNotFoundException(
                                "DNS record not found for hostname: missing.com"
                        )
                );

        mockMvc.perform(
                get("/api/dns/missing.com")
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
                .value(
                        "DNS record not found for hostname: missing.com"
                ));
    }

    @Test
    void shouldReturn204WhenDeletingRecord()
            throws Exception {

        doNothing()
                .when(dnsService)
                .deleteRecord(
                        eq("example.com"),
                        eq(DnsRecordType.A),
                        eq("192.168.1.1")
                );

        mockMvc.perform(
                delete("/api/dns/example.com")
                        .param("type", "A")
                        .param(
                                "value",
                                "192.168.1.1"
                        )
        )
        .andExpect(status().isNoContent());

        verify(dnsService).deleteRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1"
        );
    }
}