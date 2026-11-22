package de.yusufakcay.contractmanagement.document.web;

import java.io.IOException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import de.yusufakcay.contractmanagement.document.service.DocumentSearchService;
import de.yusufakcay.contractmanagement.document.service.DocumentService;
import de.yusufakcay.contractmanagement.document.service.UnsupportedDocumentException;
import de.yusufakcay.contractmanagement.shared.security.ReadAccess;
import de.yusufakcay.contractmanagement.shared.security.WriteAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "Documents", description = "Upload PDF/DOCX and ask questions about them (RAG with pgvector + Ollama)")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

	private final DocumentService service;
	private final DocumentSearchService searchService;

	@Operation(summary = "List uploaded documents (paged)")
	@GetMapping
	@ReadAccess
	public Page<DocumentDto> list(@ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.list(pageable);
	}

	@Operation(summary = "Upload a PDF or DOCX (max. 10 MB); text is split, embedded and stored in pgvector")
	@ApiResponse(responseCode = "201")
	@ApiResponse(responseCode = "413", description = "File larger than 10 MB")
	@ApiResponse(responseCode = "415", description = "Neither PDF nor DOCX")
	@ApiResponse(responseCode = "422", description = "No extractable text (e.g. scanned PDF)")
	@ApiResponse(responseCode = "503", description = "Embedding model (Ollama) not reachable")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@WriteAccess
	public ResponseEntity<DocumentDto> upload(@RequestPart MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new UnsupportedDocumentException(HttpStatus.BAD_REQUEST, "File is empty");
		}
		String filename = StringUtils.cleanPath(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document");
		DocumentDto document = service.upload(StringUtils.getFilename(filename), file.getBytes());
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}").buildAndExpand(document.id()).toUri()).body(document);
	}

	@Operation(summary = "Delete a document including its chunks")
	@ApiResponse(responseCode = "404", description = "Document not found")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@WriteAccess
	public void delete(@PathVariable @Positive Long id) {
		service.delete(id);
	}

	@Operation(summary = "Ask a question; the answer is generated only from the best matching chunks, which are returned as sources")
	@ApiResponse(responseCode = "503", description = "Language model (Ollama) not reachable")
	@PostMapping("/ask")
	@ReadAccess
	public AnswerDto ask(@Valid @RequestBody QuestionRequest request) {
		return searchService.ask(request.question());
	}
}
