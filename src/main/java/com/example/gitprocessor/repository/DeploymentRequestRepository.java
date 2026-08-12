package com.example.gitprocessor.repository;

import com.example.gitprocessor.model.DeploymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeploymentRequestRepository extends JpaRepository<DeploymentRequest, Long> {

    List<DeploymentRequest> findByEnvironmentAndStatusOrderByUploadTimeDesc(
            String environment, String status);

    long countByStatus(String status);

    long countByStatusAndEnvironment(String status, String environment);

    long countByDeploymentDate(LocalDate deploymentDate);

    @Query("SELECT r FROM DeploymentRequest r WHERE " +
           "(:environment IS NULL OR UPPER(r.environment) = UPPER(:environment)) AND " +
           "(:status      IS NULL OR UPPER(r.status)      = UPPER(:status))      AND " +
           "(:fromDate    IS NULL OR r.deploymentDate >= :fromDate)               AND " +
           "(:toDate      IS NULL OR r.deploymentDate <= :toDate)                 " +
           "ORDER BY r.uploadTime DESC")
    List<DeploymentRequest> searchDeployments(
            @Param("environment") String environment,
            @Param("status")      String status,
            @Param("fromDate")    LocalDate fromDate,
            @Param("toDate")      LocalDate toDate);
}