package de.yusufakcay.contractmanagement.document.web;

/** One retrieved chunk; index matches the [n] citations in the answer, page is null for DOCX. */
public record SourceDto(int index, Long documentId, String filename, Integer page, String excerpt, double score) {}
