package com.mini.dns.api.service;

import com.mini.dns.api.dto.CreateDnsRecordRequest;
import com.mini.dns.api.dto.DnsResolutionResponse;
import com.mini.dns.api.entity.DnsRecord;
import com.mini.dns.api.entity.DnsRecordType;
import com.mini.dns.api.exception.DnsConflictException;
import com.mini.dns.api.exception.DnsRecordNotFoundException;
import com.mini.dns.api.exception.InvalidDnsRecordException;
import com.mini.dns.api.repository.DnsRecordRepository;
import com.mini.dns.api.validation.DnsValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class DnsServiceTest {

    @Mock
    private DnsRecordRepository repository;

    @Mock
    private DnsValidator validator;

    @InjectMocks
    private DnsService dnsService;

    @BeforeEach
    void setUp() {

        lenient()
                .when(validator.isValidHostname(anyString()))
                .thenReturn(true);

        lenient()
                .when(validator.isValidIpv4(anyString()))
                .thenReturn(true);
    }

    @Test
    void shouldCreateARecord() {

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.A,
                "example.com",
                "192.168.1.1"
        );

        when(repository.findByHostnameIgnoreCase("example.com"))
                .thenReturn(List.of());

        when(repository.save(any(DnsRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = dnsService.createRecord(request);

        assertEquals("example.com", response.getHostname());
        assertEquals(DnsRecordType.A, response.getType());
        assertEquals("192.168.1.1", response.getValue());

        verify(repository).save(any(DnsRecord.class));
    }

    @Test
    void shouldAllowMultipleARecordsForSameHostname() {

        DnsRecord existing = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1",
                null
        );

        when(repository.findByHostnameIgnoreCase("example.com"))
                .thenReturn(List.of(existing));

        when(repository.save(any(DnsRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.A,
                "example.com",
                "192.168.1.2"
        );

        var response = dnsService.createRecord(request);

        assertEquals("192.168.1.2", response.getValue());

        verify(repository).save(any(DnsRecord.class));
    }

    @Test
    void shouldRejectDuplicateARecord() {

        DnsRecord existing = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1",
                null
        );

        when(repository.findByHostnameIgnoreCase("example.com"))
                .thenReturn(List.of(existing));

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.A,
                "example.com",
                "192.168.1.1"
        );

        assertThrows(
                DnsConflictException.class,
                () -> dnsService.createRecord(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectCnameWhenARecordExists() {

        DnsRecord existingA = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1",
                null
        );

        when(repository.findByHostnameIgnoreCase("example.com"))
                .thenReturn(List.of(existingA));

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.CNAME,
                "example.com",
                "target.com"
        );

        assertThrows(
                DnsConflictException.class,
                () -> dnsService.createRecord(request)
        );
    }

    @Test
    void shouldResolveARecord() {

        DnsRecord first = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1",
                null
        );

        DnsRecord second = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.2",
                null
        );

        when(repository.findByHostnameIgnoreCase("example.com"))
                .thenReturn(List.of(first, second));

        DnsResolutionResponse response =
                dnsService.resolveHostname("example.com");

        assertEquals("example.com", response.getHostname());

        assertEquals(
                2,
                response.getResolvedIps().size()
        );

        assertTrue(
                response.getResolvedIps()
                        .contains("192.168.1.1")
        );

        assertTrue(
                response.getResolvedIps()
                        .contains("192.168.1.2")
        );

        assertEquals(
                DnsRecordType.A,
                response.getRecordType()
        );
    }

    @Test
    void shouldResolveCnameChain() {

        DnsRecord alias = new DnsRecord(
                "alias.com",
                DnsRecordType.CNAME,
                "example.com",
                null
        );

        DnsRecord target = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.10",
                null
        );

        when(repository.findByHostnameIgnoreCase("alias.com"))
                .thenReturn(List.of(alias));

        when(repository.findByHostnameIgnoreCase("example.com"))
                .thenReturn(List.of(target));

        DnsResolutionResponse response =
                dnsService.resolveHostname("alias.com");

        assertEquals(
                "192.168.1.10",
                response.getResolvedIps().get(0)
        );

        assertEquals(
                DnsRecordType.CNAME,
                response.getRecordType()
        );
    }

    @Test
    void shouldReturnNotFoundWhenHostnameDoesNotExist() {

        when(repository.findByHostnameIgnoreCase("missing.com"))
                .thenReturn(List.of());

        assertThrows(
                DnsRecordNotFoundException.class,
                () -> dnsService.resolveHostname("missing.com")
        );
    }

    @Test
    void shouldRejectCircularCname() {

        DnsRecord bToC = new DnsRecord(
                "b.com",
                DnsRecordType.CNAME,
                "c.com",
                null
        );

        DnsRecord cToA = new DnsRecord(
                "c.com",
                DnsRecordType.CNAME,
                "a.com",
                null
        );

        when(repository.findByHostnameIgnoreCase("a.com"))
                .thenReturn(List.of());

        when(repository.findByHostnameIgnoreCase("b.com"))
                .thenReturn(List.of(bToC));

        when(repository.findByHostnameIgnoreCase("c.com"))
                .thenReturn(List.of(cToA));

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.CNAME,
                "a.com",
                "b.com"
        );

        assertThrows(
                DnsConflictException.class,
                () -> dnsService.createRecord(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingRecord() {

        DnsRecord record = new DnsRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1",
                null
        );

        when(
                repository
                        .findByHostnameIgnoreCaseAndTypeAndValueIgnoreCase(
                                "example.com",
                                DnsRecordType.A,
                                "192.168.1.1"
                        )
        ).thenReturn(Optional.of(record));

        dnsService.deleteRecord(
                "example.com",
                DnsRecordType.A,
                "192.168.1.1"
        );

        verify(repository).delete(record);
    }

    private CreateDnsRecordRequest createRequest(
            DnsRecordType type,
            String hostname,
            String value) {

        CreateDnsRecordRequest request =
                new CreateDnsRecordRequest();

        request.setType(type);
        request.setHostname(hostname);
        request.setValue(value);

        return request;
    }
    @Test
    void shouldRejectInvalidIpv4Address() {

        when(validator.isValidIpv4("999.999.999.999"))
                .thenReturn(false);

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.A,
                "example.com",
                "999.999.999.999"
        );

        assertThrows(
                InvalidDnsRecordException.class,
                () -> dnsService.createRecord(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidHostname() {

        when(validator.isValidHostname("invalid hostname"))
                .thenReturn(false);

        CreateDnsRecordRequest request = createRequest(
                DnsRecordType.A,
                "invalid hostname",
                "192.168.1.1"
        );

        assertThrows(
                InvalidDnsRecordException.class,
                () -> dnsService.createRecord(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingRecord() {

        when(repository.findByHostnameIgnoreCaseAndTypeAndValueIgnoreCase(
                "missing.com",
                DnsRecordType.A,
                "192.168.1.1"
        )).thenReturn(Optional.empty());

        assertThrows(
                DnsRecordNotFoundException.class,
                () -> dnsService.deleteRecord(
                        "missing.com",
                        DnsRecordType.A,
                        "192.168.1.1"
                )
        );

        verify(repository, never()).delete(any());
    }

    @Test
    void shouldIgnoreExpiredRecordDuringResolution() {

        DnsRecord expiredRecord = new DnsRecord(
                "expired.com",
                DnsRecordType.A,
                "192.168.1.50",
                1
        );

        expiredRecord.setExpiresAt(
                java.time.LocalDateTime.now().minusSeconds(5)
        );

        when(repository.findByHostnameIgnoreCase("expired.com"))
                .thenReturn(List.of(expiredRecord));

        assertThrows(
                DnsRecordNotFoundException.class,
                () -> dnsService.resolveHostname("expired.com")
        );
    }

    
}