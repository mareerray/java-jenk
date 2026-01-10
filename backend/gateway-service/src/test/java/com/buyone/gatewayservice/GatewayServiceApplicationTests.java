package com.buyone.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ContextConfiguration;


@SpringBootTest
@ContextConfiguration(classes = {})
class GatewayServiceApplicationTests {
    
    @MockBean
    private ReactiveJwtDecoder jwtDecoder;
    
	@Test
	void contextLoads() {
	}

}
