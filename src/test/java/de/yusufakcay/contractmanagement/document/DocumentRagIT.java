package de.yusufakcay.contractmanagement.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.ResourceAccessException;
import com.jayway.jsonpath.JsonPath;
import de.yusufakcay.contractmanagement.TestcontainersConfiguration;
import de.yusufakcay.contractmanagement.document.domain.UploadedDocumentRepository;
import de.yusufakcay.contractmanagement.document.mcp.DocumentMcpTools;
import jakarta.validation.ConstraintViolationException;

/**
 * Real pgvector (Testcontainers), real PDF/DOCX parsing and chunking; only the models are replaced:
 * a deterministic bag-of-words embedding (shared words → similar vectors) and a mocked chat model.
 */
@SpringBootTest(properties = { "spring.rabbitmq.listener.simple.auto-startup=false", "app.documents.min-similarity=0" })
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, DocumentRagIT.FakeEmbeddingConfig.class })
class DocumentRagIT {

	@Autowired
	private MockMvcTester mvc;
	@Autowired
	private UploadedDocumentRepository repository;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private DocumentMcpTools mcpTools;
	@MockitoBean
	private ChatModel chatModel;
	@MockitoBean
	private RabbitTemplate rabbitTemplate;
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void cleanUp() {
		given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());
		jdbc.sql("DELETE FROM vector_store").update();
		repository.deleteAll();
	}

	@Test
	void uploadedPdfIsChunkedAndAnswersCiteTheMatchingPage() throws Exception {
		var upload = mvc.post().uri("/api/documents").with(clerk()).multipart().file(file("src/main/resources/demo/avb-hausrat-demo.pdf")).exchange();
		assertThat(upload).hasStatus(HttpStatus.CREATED).headers().containsHeader("Location");
		assertThat(upload).bodyJson().extractingPath("$.createdBy").isEqualTo("clerk");
		assertThat(jdbc.sql("SELECT count(*) FROM vector_store").query(Long.class).single()).isGreaterThan(1);

		given(chatModel.call(any(Prompt.class))).willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Nur mit Zusatzbaustein [1]")))));

		var answer = mvc.post().uri("/api/documents/ask").with(auditor()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"question\":\"Ist mein Fahrrad gegen Diebstahl versichert?\"}").exchange();

		assertThat(answer).hasStatusOk();
		assertThat(answer).bodyJson().extractingPath("$.answer").isEqualTo("Nur mit Zusatzbaustein [1]");
		assertThat(answer).bodyJson().extractingPath("$.sources[0].filename").isEqualTo("avb-hausrat-demo.pdf");
		assertThat(answer).bodyJson().extractingPath("$.sources[0].page").isEqualTo(1);
		assertThat(answer).bodyJson().extractingPath("$.sources[0].excerpt").asString().contains("Fahrrad");

		ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
		then(chatModel).should().call(prompt.capture());
		assertThat(prompt.getValue().getContents()).contains("Fahrradschloss", "Ist mein Fahrrad gegen Diebstahl versichert?");
	}

	@Test
	void deletingDocumentRemovesItsChunks() throws Exception {
		var upload = mvc.post().uri("/api/documents").with(clerk()).multipart().file(file("samples/avb-glas-demo.docx")).exchange();
		assertThat(upload).hasStatus(HttpStatus.CREATED);
		assertThat(mcpTools.searchDocuments("Glasbruch Ceranfeld")).isNotEmpty()
			.allSatisfy(source -> assertThat(source.filename()).isEqualTo("avb-glas-demo.docx"));

		Integer id = JsonPath.read(upload.getResponse().getContentAsString(), "$.id");
		assertThat(mvc.delete().uri("/api/documents/{id}", id).with(clerk())).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mcpTools.searchDocuments("Glasbruch Ceranfeld")).isEmpty();
		assertThat(mvc.delete().uri("/api/documents/{id}", id).with(clerk())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void askingWithoutDocumentsSkipsTheModel() {
		var answer = mvc.post().uri("/api/documents/ask").with(auditor()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"question\":\"Was ist versichert?\"}").exchange();

		assertThat(answer).hasStatusOk();
		assertThat(answer).bodyJson().extractingPath("$.answer").isNull();
		then(chatModel).should(org.mockito.Mockito.never()).call(any(Prompt.class));
	}

	@Test
	void rejectsInvalidUploadsAndMissingWriteRole() throws Exception {
		assertThat(mvc.post().uri("/api/documents").with(auditor()).multipart().file(file("samples/avb-glas-demo.docx")))
			.hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.post().uri("/api/documents").with(clerk()).multipart()
			.file(new MockMultipartFile("file", "notes.pdf", "application/pdf", "just text".getBytes())))
			.hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		assertThat(mvc.post().uri("/api/documents").with(clerk()).multipart()
			.file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])))
			.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.post().uri("/api/documents/ask").with(auditor()).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\" \"}"))
			.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void unreachableModelIsReportedAs503() throws Exception {
		assertThat(mvc.post().uri("/api/documents").with(clerk()).multipart().file(file("samples/avb-glas-demo.docx"))).hasStatus(HttpStatus.CREATED);
		given(chatModel.call(any(Prompt.class))).willThrow(new ResourceAccessException("Connection refused"));

		assertThat(mvc.post().uri("/api/documents/ask").with(auditor()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"question\":\"Was ist bei Glasbruch versichert?\"}")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void mcpSearchRejectsBlankOrOversizedQuery() {
		assertThatThrownBy(() -> mcpTools.searchDocuments(" ")).isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> mcpTools.searchDocuments("x".repeat(501))).isInstanceOf(ConstraintViolationException.class);
	}

	private static MockMultipartFile file(String path) throws Exception {
		Path file = Path.of(path);
		return new MockMultipartFile("file", file.getFileName().toString(), MediaType.APPLICATION_OCTET_STREAM_VALUE, Files.readAllBytes(file));
	}

	private static JwtRequestPostProcessor clerk() {
		return jwt().jwt(j -> j.claim("preferred_username", "clerk"))
			.authorities(new SimpleGrantedAuthority("ROLE_contract-read"), new SimpleGrantedAuthority("ROLE_contract-write"));
	}

	private static JwtRequestPostProcessor auditor() {
		return jwt().jwt(j -> j.claim("preferred_username", "auditor")).authorities(new SimpleGrantedAuthority("ROLE_contract-read"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeEmbeddingConfig {

		@Bean
		@Primary
		EmbeddingModel fakeEmbeddingModel() {
			return new BagOfWordsEmbeddingModel();
		}
	}

	/** Hashes words (≥ 4 letters) into 1024 buckets and normalizes, so cosine similarity ≈ word overlap. */
	static class BagOfWordsEmbeddingModel implements EmbeddingModel {

		@Override
		public EmbeddingResponse call(EmbeddingRequest request) {
			List<String> texts = request.getInstructions();
			return new EmbeddingResponse(java.util.stream.IntStream.range(0, texts.size())
				.mapToObj(i -> new Embedding(vector(texts.get(i)), i)).toList());
		}

		@Override
		public float[] embed(Document document) {
			return vector(document.getText());
		}

		@Override
		public int dimensions() {
			return 1024;
		}

		private static float[] vector(String text) {
			float[] vector = new float[1024];
			for (String word : text.toLowerCase(Locale.GERMAN).split("[^\\p{L}]+")) {
				if (word.length() >= 4) {
					vector[Math.floorMod(word.hashCode(), vector.length)] += 1;
				}
			}
			double norm = 0;
			for (float v : vector) {
				norm += v * v;
			}
			float scale = norm == 0 ? 0 : (float) (1 / Math.sqrt(norm));
			for (int i = 0; i < vector.length; i++) {
				vector[i] *= scale;
			}
			return vector;
		}
	}
}
