package de.yusufakcay.contractmanagement.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.documents")
public record DocumentProperties(
		@Positive int embeddingDimensions,
		@Positive @Max(20) int topK,
		@DecimalMin("0") @DecimalMax("1") double minSimilarity,
		Resource demoDocument) {}
