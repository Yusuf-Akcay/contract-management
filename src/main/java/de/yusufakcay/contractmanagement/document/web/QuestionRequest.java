package de.yusufakcay.contractmanagement.document.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionRequest(@NotBlank @Size(max = 500) String question) {}
