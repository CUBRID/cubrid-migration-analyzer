/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalyzerFailure {
    private AnalyzerFailureStage failureStage;
    private String statementType;
    private String statementId;
    private String objectName;
    private String sql;
    private String reason;
    private float estimatedCost;
    private final List<AnalyzerCostDetail> costDetails =
            new ArrayList<AnalyzerCostDetail>();

    public AnalyzerFailureStage getFailureStage() {
        return failureStage;
    }

    public void setFailureStage(AnalyzerFailureStage failureStage) {
        this.failureStage = failureStage;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public float getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(float estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public List<AnalyzerCostDetail> getCostDetails() {
        return Collections.unmodifiableList(costDetails);
    }

    public void clearCostDetails() {
        costDetails.clear();
    }

    public void addCostDetail(AnalyzerCostDetail costDetail) {
        if (costDetail != null) {
            costDetails.add(costDetail);
        }
    }
}
