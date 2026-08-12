package com.example.gitprocessor.controller;

import com.example.gitprocessor.model.DeploymentRequest;
import com.example.gitprocessor.model.Environment;
import com.example.gitprocessor.model.FileValidationResult;
import com.example.gitprocessor.model.ProcessingResult;
import com.example.gitprocessor.service.DeploymentRequestService;
import com.example.gitprocessor.service.ExcelProcessorService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * REST endpoints for the Deployment Request module.
 *
 *   POST /api/deployment/upload              → Developer uploads a deployment request
 *   GET  /api/deployment/search              → Deployer searches requests (filtered)
 *   GET  /api/deployment/dashboard           → Aggregate stat counts for dashboard cards
 *   GET  /api/deployment/pending/{env}       → All PENDING requests for one environment
 *   GET  /api/deployment/download/{id}       → Download the original uploaded Excel
 *   POST /api/deployment/complete/{id}       → Mark request as DEPLOYED
 *   POST /api/deployment/process-requests    → Bulk-process selected requests (from disk)
 */
@Controller
public class DeploymentController {

    private static final Logger log = LoggerFactory.getLogger(DeploymentController.class);
    private static final String SESSION_RESULT_KEY = "processingResult";

    private final DeploymentRequestService deploymentService;
    private final ExcelProcessorService    excelService;

    public DeploymentController(DeploymentRequestService deploymentService,
                                ExcelProcessorService    excelService) {
        this.deploymentService = deploymentService;
        this.excelService      = excelService;
    }

    // ── Developer: upload a deployment request ────────────────────────────────

    @PostMapping("/api/deployment/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadRequest(
            @RequestParam("developerName")                          String        developerName,
            @RequestParam("environment")                            String        environment,
            @RequestParam(value = "deploymentDate",  required = false) String    deploymentDate,
            @RequestParam(value = "deploymentTime",  required = false) String    deploymentTime,
            @RequestParam(value = "description",     required = false) String    description,
            @RequestParam(value = "remarks",         required = false) String    remarks,
            @RequestParam("file")                                   MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        // Basic input validation
        if (developerName == null || developerName.isBlank()) {
            response.put("success", false);
            response.put("message", "Developer Name is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (environment == null || environment.isBlank()) {
            response.put("success", false);
            response.put("message", "Please Select Target Environment.");
            return ResponseEntity.badRequest().body(response);
        }
        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please upload an Excel file.");
            return ResponseEntity.badRequest().body(response);
        }

        // Validate Excel columns
        FileValidationResult vr = excelService.validateFile(file);
        if (!vr.isValid()) {
            response.put("success", false);
            response.put("invalidFormat", true);
            response.put("missingColumns", vr.getMissingColumns());
            response.put("message", "Invalid Excel Format. Please use the standard template.");
            return ResponseEntity.badRequest().body(response);
        }

        // Validate mandatory data fields row-by-row
        List<String> rowErrors = excelService.validateRowsForDeployment(file);
        if (!rowErrors.isEmpty()) {
            response.put("success", false);
            response.put("message", "Deployment Request Rejected - Invalid Records Found");
            response.put("rowErrors", rowErrors);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            DeploymentRequest req = deploymentService.createRequest(
                    developerName, environment, deploymentDate,
                    deploymentTime, description, remarks, file);

            response.put("success",   true);
            response.put("requestId", req.getRequestId());
            response.put("message",   "Deployment Request Generated Successfully");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to store deployment file: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "File storage error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("Unexpected error creating deployment request: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to create request: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── Deployer: search deployment requests ──────────────────────────────────

    @GetMapping("/api/deployment/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchDeployments(
            @RequestParam(value = "environment", required = false) String environment,
            @RequestParam(value = "status",      defaultValue = "ALL") String status,
            @RequestParam(value = "fromDate",    required = false) String fromDate,
            @RequestParam(value = "toDate",      required = false) String toDate) {

        Map<String, Object> response = new HashMap<>();
        try {
            List<DeploymentRequest> results =
                    deploymentService.searchDeployments(environment, status, fromDate, toDate);
            response.put("success", true);
            response.put("results", results);
            response.put("count",   results.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Search error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── Dashboard summary stats ───────────────────────────────────────────────

    @GetMapping("/api/deployment/dashboard")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("stats",   deploymentService.getDashboardStats());
        return ResponseEntity.ok(response);
    }

    // ── Pending requests for an environment (Bulk Upload import) ─────────────

    @GetMapping("/api/deployment/pending/{environment}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPendingByEnvironment(
            @PathVariable String environment) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("results", deploymentService.getPendingByEnvironment(environment));
        return ResponseEntity.ok(response);
    }

    // ── Download the original uploaded Excel ─────────────────────────────────

    @GetMapping("/api/deployment/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        try {
            byte[] bytes = deploymentService.downloadFile(id);
            DeploymentRequest req = deploymentService.getById(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + req.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) {
            log.error("Download failed for id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error downloading deployment file id={}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Mark deployment as completed ──────────────────────────────────────────

    @PostMapping("/api/deployment/complete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markDeployed(
            @PathVariable Long id,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        // Only DEPLOYER role can mark as deployed
        String role = (String) session.getAttribute("loggedInRole");
        if (!"DEPLOYER".equals(role)) {
            response.put("success", false);
            response.put("message", "Only Deployers can mark deployments as completed.");
            return ResponseEntity.status(403).body(response);
        }

        // Auto-populate deployedBy from logged-in session user
        String deployedBy = (String) session.getAttribute("loggedInDisplay");
        if (deployedBy == null || deployedBy.isBlank()) {
            deployedBy = (String) session.getAttribute("loggedInUser");
        }

        try {
            DeploymentRequest req = deploymentService.markDeployed(id, deployedBy);
            response.put("success",     true);
            response.put("requestId",   req.getRequestId());
            response.put("deployedBy",  req.getDeployedBy());
            response.put("deployedTime", req.getDeployedTime().toString());
            response.put("message",     "Deployment marked as completed.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Mark deployed failed for id={}: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── Reject deployment ─────────────────────────────────────────────────────

    @PostMapping("/api/deployment/reject/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectDeployment(
            @PathVariable Long id,
            @RequestParam("comment") String comment,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        // Only DEPLOYER role can reject
        String role = (String) session.getAttribute("loggedInRole");
        if (!"DEPLOYER".equals(role)) {
            response.put("success", false);
            response.put("message", "Only Deployers can reject deployments.");
            return ResponseEntity.status(403).body(response);
        }

        if (comment == null || comment.isBlank()) {
            response.put("success", false);
            response.put("message", "Rejection comment is mandatory.");
            return ResponseEntity.badRequest().body(response);
        }

        String rejectedBy = (String) session.getAttribute("loggedInDisplay");
        if (rejectedBy == null || rejectedBy.isBlank()) {
            rejectedBy = (String) session.getAttribute("loggedInUser");
        }

        try {
            DeploymentRequest req = deploymentService.markRejected(id, rejectedBy, comment);
            response.put("success",   true);
            response.put("requestId", req.getRequestId());
            response.put("message",   "Deployment request rejected.");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Reject failed for id={}: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── Bulk Upload: process directly from stored deployment requests ─────────

    /**
     * Takes a list of deployment-request IDs, reads their stored Excel files
     * from the server filesystem, merges them, and processes them exactly like
     * the normal /api/process flow.  The result is stored in the session so all
     * download endpoints work as usual.
     */
    @PostMapping("/api/deployment/process-requests")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processFromRequests(
            @RequestParam("ids") List<Long> ids,
            @RequestParam("environment") String environmentName,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        Environment environment = Environment.fromName(environmentName);
        if (environment == null) {
            response.put("success", false);
            response.put("message", "Please select a valid environment.");
            return ResponseEntity.badRequest().body(response);
        }
        if (ids == null || ids.isEmpty()) {
            response.put("success", false);
            response.put("message", "No deployment requests selected.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<DeploymentRequest> requests = deploymentService.getByIds(ids);

            // Build lists of paths and names to pass to the service
            List<java.nio.file.Path> paths = new ArrayList<>();
            List<String>             names = new ArrayList<>();
            for (DeploymentRequest req : requests) {
                paths.add(Paths.get(req.getFilePath()));
                names.add(req.getFileName() != null ? req.getFileName() : req.getRequestId() + ".xlsx");
            }

            ProcessingResult result =
                    excelService.processFromStoredFiles(paths, names, environment);

            session.setAttribute(SESSION_RESULT_KEY, result);

            response.put("success",      true);
            response.put("message",
                    "Processed " + requests.size() + " deployment request(s). " +
                    result.getSummary().getTotalRows() + " rows merged.");
            response.put("summary",      result.getSummary());
            response.put("logMessages",  result.getLogMessages());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("process-requests failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
