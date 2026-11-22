package de.yusufakcay.contractmanagement.document.domain;

import de.yusufakcay.contractmanagement.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "uploaded_document")
@Getter
@Setter
@NoArgsConstructor
public class UploadedDocument extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private String filename;

	@Column(nullable = false, updatable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false, updatable = false)
	private long size;

	@Column(name = "chunk_count", nullable = false)
	private int chunks;
}
