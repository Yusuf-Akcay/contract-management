package de.yusufakcay.contractmanagement.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

class DocumentTextExtractorTest {

	private final DocumentTextExtractor extractor = new DocumentTextExtractor();

	@Test
	void readsPdfPageByPageWithPageNumbers() throws Exception {
		byte[] pdf = Files.readAllBytes(Path.of("src/main/resources/demo/avb-hausrat-demo.pdf"));

		String type = extractor.detectType(pdf, "avb.pdf");

		assertThat(type).isEqualTo(DocumentTextExtractor.PDF);
		assertThat(extractor.extract(pdf, "avb.pdf", type))
			.hasSize(2)
			.extracting(page -> page.getMetadata().get(DocumentTextExtractor.PAGE))
			.containsExactly(1, 2);
		assertThat(extractor.extract(pdf, "avb.pdf", type).getFirst().getText()).contains("Elementarschäden wie Überschwemmung").doesNotContain("  ");
	}

	@Test
	void readsDocx() throws Exception {
		byte[] docx = Files.readAllBytes(Path.of("samples/avb-glas-demo.docx"));

		String type = extractor.detectType(docx, "glas.docx");

		assertThat(type).isEqualTo(DocumentTextExtractor.DOCX);
		assertThat(extractor.extract(docx, "glas.docx", type)).singleElement()
			.extracting(Document::getText).asString().contains("Glaskeramik-Kochflächen");
	}

	@Test
	void detectsTypeFromContentNotFromFilename() {
		byte[] xml = "<?xml version=\"1.0\"?><project/>".getBytes();

		assertThatThrownBy(() -> extractor.detectType(xml, "fake.pdf"))
			.isInstanceOfSatisfying(ErrorResponseException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
	}
}
