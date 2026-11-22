package de.yusufakcay.contractmanagement.document.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DocumentConfig {

	static final String SYSTEM_PROMPT = """
		Du bist ein Assistent für Versicherungsbedingungen. Beantworte die Frage ausschließlich mit den nummerierten Auszügen.
		- Antworte immer auf Deutsch, in zwei bis vier Sätzen.
		- Beginne mit der direkten Antwort, zum Beispiel "Ja, aber nur ..." oder "Nein, ...".
		- Nenne alle Bedingungen, Grenzen und Beträge aus den Auszügen, die zur Frage passen.
		- Setze hinter jede Aussage die Nummer des Auszugs in eckigen Klammern, zum Beispiel [1].
		- Steht die Antwort nicht in den Auszügen, antworte: "Dazu finde ich in den Dokumenten nichts." Rate nie.
		""";

	@Bean
	ChatClient documentChatClient(ChatClient.Builder builder) {
		return builder.defaultSystem(SYSTEM_PROMPT).build();
	}

	/** ~300 tokens per chunk: roughly one clause, so the top hits stay on topic. */
	@Bean
	TokenTextSplitter documentSplitter() {
		return TokenTextSplitter.builder().withChunkSize(300).withMinChunkSizeChars(200).build();
	}
}
