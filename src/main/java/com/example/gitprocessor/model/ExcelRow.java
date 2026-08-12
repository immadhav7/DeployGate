package com.example.gitprocessor.model;

import java.io.Serializable;

/**
 * Represents a single row from the uploaded Excel file.
 * Supports the 9-column input format + processing output columns.
 */
public class ExcelRow implements Serializable {

    private static final long serialVersionUID = 2L;

    // ── Input columns ─────────────────────────────────────────────────────────
    private int    slNo;
    private String files;
    private String originalType;
    private String pushedBy;
    private String releaseBranch;
    private String targetEnv;
    private String date;
    private String team;
    private String crIncReason;

    // ── Processing output columns ─────────────────────────────────────────────
    private String gitType;
    private String finalType;
    private String validationStatus;
    private String validationReason;
    private String sourceFile;

    public ExcelRow() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int    getSlNo()             { return slNo; }
    public void   setSlNo(int v)        { this.slNo = v; }

    public String getFiles()            { return files; }
    public void   setFiles(String v)    { this.files = v; }

    public String getOriginalType()     { return originalType; }
    public void   setOriginalType(String v) { this.originalType = v; }

    public String getPushedBy()         { return pushedBy; }
    public void   setPushedBy(String v) { this.pushedBy = v; }

    public String getReleaseBranch()    { return releaseBranch; }
    public void   setReleaseBranch(String v) { this.releaseBranch = v; }

    public String getTargetEnv()        { return targetEnv; }
    public void   setTargetEnv(String v){ this.targetEnv = v; }

    public String getDate()             { return date; }
    public void   setDate(String v)     { this.date = v; }

    public String getTeam()             { return team; }
    public void   setTeam(String v)     { this.team = v; }

    public String getCrIncReason()      { return crIncReason; }
    public void   setCrIncReason(String v) { this.crIncReason = v; }

    public String getGitType()          { return gitType; }
    public void   setGitType(String v)  { this.gitType = v; }

    public String getFinalType()        { return finalType; }
    public void   setFinalType(String v){ this.finalType = v; }

    public String getValidationStatus() { return validationStatus; }
    public void   setValidationStatus(String v) { this.validationStatus = v; }

    public String getValidationReason() { return validationReason; }
    public void   setValidationReason(String v) { this.validationReason = v; }

    public String getSourceFile()       { return sourceFile; }
    public void   setSourceFile(String v) { this.sourceFile = v; }
}