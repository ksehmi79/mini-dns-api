package com.mini.dns.api.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component 
public class DnsValidator {

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*\\.?$"
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}" +
            "(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$"
    );
    
    public boolean isValidHostname(String hostname){
        if(hostname == null || hostname.isBlank()){
            return false;
        }
        return HOSTNAME_PATTERN.matcher(hostname).matches();

    }

    public boolean isValidIpv4(String ip){
        if(ip == null || ip.isBlank()){
            return false;
        }

        return IPV4_PATTERN.matcher(ip).matches();

    }
    
}
