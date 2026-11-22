package de.yusufakcay.contractmanagement.document.web;

import java.util.List;

/** answer is null when no chunk is similar enough, so the client can tell "nothing found" from a model answer. */
public record AnswerDto(String answer, List<SourceDto> sources) {}
