package de.yusufakcay.contractmanagement.document.mcp;

import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import de.yusufakcay.contractmanagement.document.service.DocumentSearchService;
import de.yusufakcay.contractmanagement.document.web.SourceDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Validated
public class DocumentMcpTools {

	private final DocumentSearchService searchService;

	@McpTool(name = "search_documents",
		description = "Semantic search in uploaded documents (e.g. insurance terms). Returns the best matching text excerpts with file name and page.",
		annotations = @McpTool.McpAnnotations(readOnlyHint = true))
	public List<SourceDto> searchDocuments(@McpToolParam(description = "Question or search phrase, any language") @NotBlank @Size(max = 500) String query) {
		return searchService.search(query);
	}
}
