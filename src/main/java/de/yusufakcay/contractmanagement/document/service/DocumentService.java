package de.yusufakcay.contractmanagement.document.service;

import java.util.HashMap;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import de.yusufakcay.contractmanagement.document.domain.UploadedDocument;
import de.yusufakcay.contractmanagement.document.domain.UploadedDocumentRepository;
import de.yusufakcay.contractmanagement.document.web.DocumentDto;
import de.yusufakcay.contractmanagement.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Ingestion: detect type → extract text → split into chunks → embed + store in pgvector.
 * Metadata row and chunks are written in one transaction (PgVectorStore uses the same DataSource),
 * so a failing embedding call leaves nothing half-imported.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

	public static final String DOCUMENT_ID = "documentId";
	public static final String FILENAME = "filename";

	private final UploadedDocumentRepository repository;
	private final DocumentMapper mapper;
	private final DocumentTextExtractor extractor;
	private final TokenTextSplitter splitter;
	private final VectorStore vectorStore;

	public Page<DocumentDto> list(Pageable pageable) {
		return repository.findAll(pageable).map(mapper::toDto);
	}

	@Transactional
	public DocumentDto upload(String filename, byte[] content) {
		String type = extractor.detectType(content, filename);
		List<Document> chunks = splitter.apply(extractor.extract(content, filename, type));

		var document = new UploadedDocument();
		document.setFilename(filename);
		document.setContentType(type);
		document.setSize(content.length);
		document.setChunks(chunks.size());
		repository.saveAndFlush(document);

		vectorStore.add(chunks.stream().map(chunk -> {
			var metadata = new HashMap<>(chunk.getMetadata());
			metadata.put(DOCUMENT_ID, document.getId().toString());
			metadata.put(FILENAME, filename);
			return new Document(chunk.getText(), metadata);
		}).toList());
		return mapper.toDto(document);
	}

	@Transactional
	public void delete(Long id) {
		UploadedDocument document = repository.findById(id).orElseThrow(() -> new NotFoundException("Document %d not found".formatted(id)));
		vectorStore.delete(new FilterExpressionBuilder().eq(DOCUMENT_ID, id.toString()).build());
		repository.delete(document);
	}

	public boolean isEmpty() {
		return repository.count() == 0;
	}
}
