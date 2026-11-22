package de.yusufakcay.contractmanagement.shared.domain;

import java.time.Instant;
import java.util.Objects;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Audit columns, optimistic locking and proxy-safe equals/hashCode for all aggregates
 * (inlined instead of platform-jpa so the project builds without private dependencies).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

	@Version
	@Setter(AccessLevel.NONE)
	@Column(nullable = false)
	private Long version;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@CreatedBy
	@Column(name = "created_by", updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by")
	private String updatedBy;

	public abstract Long getId();

	public void checkVersion(Long expectedVersion) {
		if (!Objects.equals(version, expectedVersion)) {
			throw new ObjectOptimisticLockingFailureException(Hibernate.getClass(this), getId());
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
			return false;
		}
		return getId() != null && getId().equals(((AuditableEntity) other).getId());
	}

	@Override
	public int hashCode() {
		return Hibernate.getClass(this).hashCode();
	}
}
