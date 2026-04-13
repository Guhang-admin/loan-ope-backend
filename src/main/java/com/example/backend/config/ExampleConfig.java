package com.example.backend.config;

import com.example.backend.examples.timeout.ConnectionLeakExample;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class ExampleConfig {
    
    private final DataSource dataSource;
    
    public ExampleConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Bean
    public ConnectionLeakExample connectionLeakExample() {
        return new ConnectionLeakExample(dataSource);
    }
}