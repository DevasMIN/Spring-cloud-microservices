package com.example.integration.common;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class IntegrationTestCommonConfig {
    @Getter
    @Value("${api.auth}")
    private String authUrl;
    
    @Getter
    @Value("${api.users}")
    private String usersUrl;
    
    @Getter
    @Value("${api.inventory}")
    private String inventoryUrl;
    
    @Getter
    @Value("${api.order}")
    private String orderUrl;
    
    @Getter
    @Value("${api.payment}")
    private String paymentUrl;

    @Getter
    @Value("${api.balance}")
    private String balanceUrl;

    @Autowired
    private RestTemplate commonRestTemplate;

    public RestTemplate getRestTemplate() {
        return commonRestTemplate;
    }
}
