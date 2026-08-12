package com.example.gitprocessor.model;

import java.io.Serializable;

/**
 * Aggregated statistics produced after processing the Excel file(s).
 */
public class ProcessingSummary implements Serializable {

    private static final long serialVersionUID = 2L;

    private int filesUploaded;
    private int totalRows;
    private int validRows;
    private int rejectedRows;
    private String environment;
    private int customerGitCount;
    private int productGitCount;
    private int ruleGitCount;
    private int mdtCount;
    private int unknownCount;
    private int correctTypeCount;
    private int correctedTypeCount;
    private int unableToIdentifyCount;
    private int uniqueJarsCount;

    public ProcessingSummary() {}

    public int getFilesUploaded() { return filesUploaded; }
    public void setFilesUploaded(int v) { this.filesUploaded = v; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int v) { this.totalRows = v; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int v) { this.validRows = v; }

    public int getRejectedRows() { return rejectedRows; }
    public void setRejectedRows(int v) { this.rejectedRows = v; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String v) { this.environment = v; }

    public int getCustomerGitCount() { return customerGitCount; }
    public void setCustomerGitCount(int v) { this.customerGitCount = v; }

    public int getProductGitCount() { return productGitCount; }
    public void setProductGitCount(int v) { this.productGitCount = v; }

    public int getRuleGitCount() { return ruleGitCount; }
    public void setRuleGitCount(int v) { this.ruleGitCount = v; }

    public int getMdtCount() { return mdtCount; }
    public void setMdtCount(int v) { this.mdtCount = v; }

    public int getUnknownCount() { return unknownCount; }
    public void setUnknownCount(int v) { this.unknownCount = v; }

    public int getCorrectTypeCount() { return correctTypeCount; }
    public void setCorrectTypeCount(int v) { this.correctTypeCount = v; }

    public int getCorrectedTypeCount() { return correctedTypeCount; }
    public void setCorrectedTypeCount(int v) { this.correctedTypeCount = v; }

    public int getUnableToIdentifyCount() { return unableToIdentifyCount; }
    public void setUnableToIdentifyCount(int v) { this.unableToIdentifyCount = v; }

    public int getUniqueJarsCount() { return uniqueJarsCount; }
    public void setUniqueJarsCount(int v) { this.uniqueJarsCount = v; }
}