package de.yusufakcay.contractmanagement.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
		@NotBlank String exchange,
		@NotBlank String contractCreatedQueue,
		@NotBlank String contractCreatedRoutingKey) {}
