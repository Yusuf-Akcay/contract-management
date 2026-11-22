package de.yusufakcay.contractmanagement.document.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import de.yusufakcay.contractmanagement.document.config.DocumentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty("app.documents.demo-document")
@RequiredArgsConstructor
public final class DemoDocumentLoader implements ApplicationRunner {

	private final DocumentService service;
	private final DocumentProperties properties;

	@Override
	public void run(ApplicationArguments args) {
		if (!service.isEmpty()) {
			return;
		}
		try {
			var document = service.upload(properties.demoDocument().getFilename(), properties.demoDocument().getContentAsByteArray());
			log.info("Demo document {} imported ({} chunks)", document.filename(), document.chunks());
		} catch (Exception e) {
			log.warn("Demo document not imported, is Ollama running? {}", e.getMessage());
		}
	}
}
