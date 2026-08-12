package com.example.gitprocessor.service;

import com.example.gitprocessor.model.DeploymentAudit;
import com.example.gitprocessor.model.DeploymentRequest;
import com.example.gitprocessor.repository.DeploymentAuditRepository;
import com.example.gitprocessor.repository.DeploymentRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class DeploymentRequestService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentRequestService.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DeploymentRequestRepository requestRepo;
    private final DeploymentAuditRepository   auditRepo;

    @Value("${deployment.storage.path:./deployment-files}")
    private String storagePath;

    public DeploymentRequestService(DeploymentRequestRepository requestRepo,
                                    DeploymentAuditRepository   auditRepo) {
        this.requestRepo = requestRepo;
        this.auditRepo   = auditRepo;
    }

    public DeploymentRequest createRequest(String developerName, String environment,
            String deploymentDateStr, String deploymentTime,
            String description, String remarks, MultipartFile file) throws IOException {

        Path storageDir = Paths.get(storagePath, "excels");
        Files.createDirectories(storageDir);

        String ts       = LocalDateTime.now().format(TS_FMT);
        String safeName = ts + "_" + sanitize(file.getOriginalFilename());
        Path   target   = storageDir.resolve(safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        DeploymentRequest req = new DeploymentRequest();
        req.setDeveloperName(developerName.trim());
        req.setEnvironment(environment.toUpperCase());
        req.setFileName(file.getOriginalFilename());
        req.setFilePath(target.toAbsolutePath().toString());
        req.setUploadTime(LocalDateTime.now());
        req.setDeploymentTime(deploymentTime);
        req.setDescription(description);
        req.setRemarks(remarks);
        req.setStatus("PENDING");
        req.setDeploymentCompleted(false);

        if (!blank(deploymentDateStr))
            req.setDeploymentDate(LocalDate.parse(deploymentDateStr));

        DeploymentRequest saved = requestRepo.save(req);

        // Generate readable request ID from the DB-assigned primary key
        saved.setRequestId(String.format("REQ%07d", saved.getId()));
        saved = requestRepo.save(saved);

        addAudit(saved.getRequestId(), "REQUEST_CREATED", developerName,
                "Deployment request created for " + environment);

        log.info("Created {} env={} by {}", saved.getRequestId(), environment, developerName);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DeploymentRequest> searchDeployments(String environment, String status,
            String fromDateStr, String toDateStr) {
        String    env  = blank(environment) ? null : environment.toUpperCase();
        String    stat = "ALL".equalsIgnoreCase(status) || blank(status) ? null : status.toUpperCase();
        LocalDate from = blank(fromDateStr) ? null : LocalDate.parse(fromDateStr);
        LocalDate to   = blank(toDateStr)   ? null : LocalDate.parse(toDateStr);
        return requestRepo.searchDeployments(env, stat, from, to);
    }

    @Transactional(readOnly = true)
    public DeploymentRequest getById(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Deployment request not found: " + id));
    }

    public DeploymentRequest markDeployed(Long id, String deployedBy) {
        DeploymentRequest req = getById(id);
        if ("DEPLOYED".equals(req.getStatus()) || "REJECTED".equals(req.getStatus())) {
            throw new IllegalStateException("Deployment " + req.getRequestId() +
                    " is already " + req.getStatus() + " and cannot be modified.");
        }
        req.setStatus("DEPLOYED");
        req.setDeploymentCompleted(true);
        req.setDeployedBy(blank(deployedBy) ? "Deployer" : deployedBy);
        req.setDeployedTime(LocalDateTime.now());
        requestRepo.save(req);
        addAudit(req.getRequestId(), "DEPLOYMENT_COMPLETED", req.getDeployedBy(),
                "Deployment marked as completed");
        return req;
    }

    public DeploymentRequest markRejected(Long id, String rejectedBy, String comment) {
        DeploymentRequest req = getById(id);
        if ("DEPLOYED".equals(req.getStatus()) || "REJECTED".equals(req.getStatus())) {
            throw new IllegalStateException("Deployment " + req.getRequestId() +
                    " is already " + req.getStatus() + " and cannot be modified.");
        }
        req.setStatus("REJECTED");
        req.setDeployedBy(rejectedBy);
        req.setDeployedTime(LocalDateTime.now());
        req.setRejectionComment(comment);
        requestRepo.save(req);
        addAudit(req.getRequestId(), "DEPLOYMENT_REJECTED", rejectedBy,
                "Deployment rejected: " + comment);
        return req;
    }

    public byte[] downloadFile(Long id) throws IOException {
        DeploymentRequest req = getById(id);
        Path p = Paths.get(req.getFilePath());
        if (!Files.exists(p)) throw new IOException("Stored file not found: " + req.getFilePath());
        addAudit(req.getRequestId(), "FILE_DOWNLOADED", "Deployer", "File downloaded");
        return Files.readAllBytes(p);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> s = new LinkedHashMap<>();
        s.put("pending",          requestRepo.countByStatus("PENDING"));
        s.put("deployed",         requestRepo.countByStatus("DEPLOYED"));
        s.put("todayDeployments", requestRepo.countByDeploymentDate(LocalDate.now()));
        s.put("uat1Pending",      requestRepo.countByStatusAndEnvironment("PENDING", "UAT1"));
        s.put("uat2Pending",      requestRepo.countByStatusAndEnvironment("PENDING", "UAT2"));
        s.put("preprodPending",   requestRepo.countByStatusAndEnvironment("PENDING", "PREPROD"));
        s.put("prodPending",      requestRepo.countByStatusAndEnvironment("PENDING", "PROD"));
        return s;
    }

    @Transactional(readOnly = true)
    public List<DeploymentRequest> getPendingByEnvironment(String environment) {
        return requestRepo.findByEnvironmentAndStatusOrderByUploadTimeDesc(
                environment.toUpperCase(), "PENDING");
    }

    @Transactional(readOnly = true)
    public List<DeploymentRequest> getByIds(List<Long> ids) {
        return requestRepo.findAllById(ids);
    }

    private void addAudit(String requestId, String action, String user, String remarks) {
        DeploymentAudit a = new DeploymentAudit();
        a.setRequestId(requestId);
        a.setAction(action);
        a.setActionUser(user);
        a.setActionTime(LocalDateTime.now());
        a.setRemarks(remarks);
        auditRepo.save(a);
    }

    private String sanitize(String fn) {
        if (fn == null || fn.isBlank()) return "upload.xlsx";
        return fn.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}