package de.yusufakcay.contractmanagement.document.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import de.yusufakcay.contractmanagement.document.config.DocumentProperties;
import de.yusufakcay.contractmanagement.document.web.AnswerDto;
import de.yusufakcay.contractmanagement.document.web.SourceDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentSearchService {

	private static final String USER_PROMPT = """
		Auszüge:
		{context}
		Frage: {question}
		""";

	private final VectorStore vectorStore;
	private final ChatClient documentChatClient;
	private final DocumentProperties properties;

	public List<SourceDto> search(String query) {
		List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(properties.topK()).similarityThreshold(properties.minSimilarity()).build());
		return IntStream.range(0, hits.size()).mapToObj(i -> toSource(i + 1, hits.get(i))).toList();
	}

	public AnswerDto ask(String question) {
		List<SourceDto> sources = search(question);
		if (sources.isEmpty()) {
			return new AnswerDto(null, sources);
		}
		String answer = documentChatClient.prompt()
			.user(user -> user.text(USER_PROMPT).param("context", context(sources)).param("question", question))
			.call()
			.content();
		return new AnswerDto(answer, sources);
	}

	private static String context(List<SourceDto> sources) {
		return sources.stream()
			.map(source -> "<auszug nr=\"%d\">\n%s\n</auszug>".formatted(source.index(), source.excerpt()))
			.collect(Collectors.joining("\n"));
	}

	private static SourceDto toSource(int index, Document hit) {
		var metadata = hit.getMetadata();
		Object page = metadata.get(DocumentTextExtractor.PAGE);
		return new SourceDto(index,
			Long.valueOf(metadata.get(DocumentService.DOCUMENT_ID).toString()),
			String.valueOf(metadata.get(DocumentService.FILENAME)),
			page instanceof Number number ? number.intValue() : null,
			hit.getText(),
			hit.getScore() != null ? hit.getScore() : 0);
	}
}
