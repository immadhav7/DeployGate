package com.example.gitprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DEPLOYMENT_AUDIT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dep_audit_seq")
    @SequenceGenerator(name = "dep_audit_seq", sequenceName = "DEP_AUDIT_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REQUEST_ID", length = 20)
    private String requestId;

    @Column(name = "ACTION", length = 50)
    private String action;

    @Column(name = "ACTION_USER", length = 100)
    private String actionUser;

    @Column(name = "ACTION_TIME")
    private LocalDateTime actionTime;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    public DeploymentAudit() {}

    public Long          getId()                       { return id; }
    public void          setId(Long id)                { this.id = id; }
    public String        getRequestId()                { return requestId; }
    public void          setRequestId(String v)        { this.requestId = v; }
    public String        getAction()                   { return action; }
    public void          setAction(String v)           { this.action = v; }
    public String        getActionUser()               { return actionUser; }
    public void          setActionUser(String v)       { this.actionUser = v; }
    public LocalDateTime getActionTime()               { return actionTime; }
    public void          setActionTime(LocalDateTime v){ this.actionTime = v; }
    public String        getRemarks()                  { return remarks; }
    public void          setRemarks(String v)          { this.remarks = v; }
}