package com.edu.aienlighten;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("com.edu.aienlighten.mapper")
@ConfigurationPropertiesScan
public class AiEnlightenApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiEnlightenApplication.class, args);
    }
}
