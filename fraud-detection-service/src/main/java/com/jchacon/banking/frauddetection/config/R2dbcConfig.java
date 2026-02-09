package com.jchacon.banking.frauddetection.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories // Required for repository scanning
@EnableR2dbcAuditing     // Magic starts here: activates @CreatedDate and @LastModifiedDate
public class R2dbcConfig {
    // Spring Boot 3 usually handles Enum-to-String automatically,
    // but having this config class is good practice for future customizations.
}
