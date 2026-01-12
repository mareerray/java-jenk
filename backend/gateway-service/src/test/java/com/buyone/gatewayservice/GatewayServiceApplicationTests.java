package com.buyone.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ContextConfiguration;


@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.secret=test-secret-1234567890"
})
class GatewayServiceApplicationTests {
    
    @MockBean
    private ReactiveJwtDecoder jwtDecoder;
    
    @Test
    void contextLoads() {
    }
    
}
