package com.fincom.alerts.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;
	
	@Version
	private Long version;

    @NotBlank
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @NotBlank
    @Column(name = "matched_entity_name", nullable = false)
    private String matchedEntityName;

    @Min(0)
    @Max(100)
    @Column(name = "match_score")
    private int matchScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Column(name = "assigned_to")
    private String assignedTo;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "decision_note")
    private String decisionNote;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
