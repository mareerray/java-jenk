package com.buyone.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Disabled;

@Disabled("Temporarily disabled: requires MongoTemplate / MongoDB test setup")
@SpringBootTest
@EnableAutoConfiguration(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
class ProductServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
