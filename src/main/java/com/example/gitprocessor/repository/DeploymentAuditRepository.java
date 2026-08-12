package com.example.gitprocessor.repository;

import com.example.gitprocessor.model.DeploymentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeploymentAuditRepository extends JpaRepository<DeploymentAudit, Long> {
    List<DeploymentAudit> findByRequestIdOrderByActionTimeDesc(String requestId);
}