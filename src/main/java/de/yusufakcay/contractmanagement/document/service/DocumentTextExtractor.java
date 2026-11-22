package de.yusufakcay.contractmanagement.document.service;

import java.util.List;
import java.util.Map;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Detects the real file type from the bytes (the client's Content-Type is not trusted) and extracts text.
 * PDFs are read page by page so answers can cite page numbers; DOCX goes through Tika.
 */
@Component
public final class DocumentTextExtractor {

	public static final String PDF = "application/pdf";
	public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	public static final String PAGE = "page";

	private final Tika tika = new Tika();

	public String detectType(byte[] content, String filename) {
		String type = tika.detect(content, filename);
		if (!PDF.equals(type) && !DOCX.equals(type)) {
			throw new UnsupportedDocumentException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF and DOCX files are supported, got " + type);
		}
		return type;
	}

	public List<Document> extract(byte[] content, String filename, String type) {
		var resource = new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
		List<Document> pages = PDF.equals(type)
			? new PagePdfDocumentReader(resource).get().stream()
				.map(page -> new Document(normalize(page.getText()), Map.of(PAGE, page.getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER))))
				.toList()
			: new TikaDocumentReader(resource).get().stream().map(doc -> new Document(normalize(doc.getText()))).toList();
		List<Document> withText = pages.stream().filter(page -> !page.getText().isBlank()).toList();
		if (withText.isEmpty()) {
			throw new UnsupportedDocumentException(HttpStatus.UNPROCESSABLE_CONTENT, "No text found in the document (scanned PDF without OCR?)");
		}
		return withText;
	}

	/** PDFBox pads text with layout spaces; collapse them so chunks, embeddings and excerpts stay clean. */
	private static String normalize(String text) {
		return text == null ? "" : text.replaceAll("[ \\t\\u00A0]+", " ").replaceAll(" ?\\n ?", "\n").strip();
	}
}
