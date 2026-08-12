package com.example.gitprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DEPLOYMENT_REQUEST")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dep_req_seq")
    @SequenceGenerator(name = "dep_req_seq", sequenceName = "DEP_REQ_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REQUEST_ID", length = 20)
    private String requestId;

    @Column(name = "DEVELOPER_NAME", length = 100, nullable = false)
    private String developerName;

    @Column(name = "ENVIRONMENT", length = 20, nullable = false)
    private String environment;

    @Column(name = "FILE_NAME", length = 255)
    private String fileName;

    @Column(name = "FILE_PATH", length = 500)
    private String filePath;

    @Column(name = "UPLOAD_TIME")
    private LocalDateTime uploadTime;

    @Column(name = "DEPLOYMENT_DATE")
    private LocalDate deploymentDate;

    @Column(name = "DEPLOYMENT_TIME", length = 10)
    private String deploymentTime;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "STATUS", length = 20)
    private String status = "PENDING";

    @Column(name = "DEPLOYMENT_COMPLETED")
    private Boolean deploymentCompleted = false;

    @Column(name = "DEPLOYED_BY", length = 100)
    private String deployedBy;

    @Column(name = "DEPLOYED_TIME")
    private LocalDateTime deployedTime;

    @Column(name = "REJECTION_COMMENT", length = 1000)
    private String rejectionComment;

    public DeploymentRequest() {}

    public Long          getId()                       { return id; }
    public void          setId(Long id)                { this.id = id; }
    public String        getRequestId()                { return requestId; }
    public void          setRequestId(String v)        { this.requestId = v; }
    public String        getDeveloperName()            { return developerName; }
    public void          setDeveloperName(String v)    { this.developerName = v; }
    public String        getEnvironment()              { return environment; }
    public void          setEnvironment(String v)      { this.environment = v; }
    public String        getFileName()                 { return fileName; }
    public void          setFileName(String v)         { this.fileName = v; }
    public String        getFilePath()                 { return filePath; }
    public void          setFilePath(String v)         { this.filePath = v; }
    public LocalDateTime getUploadTime()               { return uploadTime; }
    public void          setUploadTime(LocalDateTime v){ this.uploadTime = v; }
    public LocalDate     getDeploymentDate()           { return deploymentDate; }
    public void          setDeploymentDate(LocalDate v){ this.deploymentDate = v; }
    public String        getDeploymentTime()           { return deploymentTime; }
    public void          setDeploymentTime(String v)   { this.deploymentTime = v; }
    public String        getDescription()              { return description; }
    public void          setDescription(String v)      { this.description = v; }
    public String        getRemarks()                  { return remarks; }
    public void          setRemarks(String v)          { this.remarks = v; }
    public String        getStatus()                   { return status; }
    public void          setStatus(String v)           { this.status = v; }
    public Boolean       getDeploymentCompleted()      { return deploymentCompleted; }
    public void          setDeploymentCompleted(Boolean v){ this.deploymentCompleted = v; }
    public String        getDeployedBy()               { return deployedBy; }
    public void          setDeployedBy(String v)       { this.deployedBy = v; }
    public LocalDateTime getDeployedTime()             { return deployedTime; }
    public void          setDeployedTime(LocalDateTime v){ this.deployedTime = v; }
    public String        getRejectionComment()          { return rejectionComment; }
    public void          setRejectionComment(String v)  { this.rejectionComment = v; }
}