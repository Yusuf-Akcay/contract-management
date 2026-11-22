package de.yusufakcay.contractmanagement.document.service;

import org.mapstruct.Mapper;
import de.yusufakcay.contractmanagement.document.domain.UploadedDocument;
import de.yusufakcay.contractmanagement.document.web.DocumentDto;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

	DocumentDto toDto(UploadedDocument document);
}
