package com.fincom.alerts.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fincom.alerts.domain.Alert;
import com.fincom.alerts.domain.AlertStatus;

public interface AlertRepository extends JpaRepository<Alert, String> {

    Optional<Alert> findByIdAndTenantId(String id, String tenantId);
    
    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId " +
    	       "AND (:status IS NULL OR a.status = :status) " +
    	       "AND (:minMatchScore IS NULL OR a.matchScore >= :minMatchScore)")
    	List<Alert> findByFilter(
    	    @Param("tenantId") String tenantId,
    	    @Param("status") AlertStatus status,
    	    @Param("minMatchScore") Integer minMatchScore
    	);
}
