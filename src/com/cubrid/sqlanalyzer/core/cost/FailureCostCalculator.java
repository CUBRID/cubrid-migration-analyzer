/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.core.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cubrid.sqlanalyzer.command.model.AnalyzerCostDetail;
import com.cubrid.sqlanalyzer.command.model.AnalyzerFailure;
import com.cubrid.sqlanalyzer.command.report.AnalyzerReport;
import com.cubrid.sqlanalyzer.core.plan.AnalyzerStatementTypes;

public class FailureCostCalculator implements AnalyzerCostCalculator {
    private static final Pattern QUERY_FORMATTING_PATTERN = Pattern.compile("[\\r\\n\\t]+");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile(" +");
    private static final Pattern KEYWORD_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern GRANT_PRIVILEGE_SEPARATOR_PATTERN = Pattern.compile("\\s*,\\s*");

    private final AnalyzerCostSettings costSettings;
    private final Map<String, Pattern> keywordPatternCache =
            new ConcurrentHashMap<String, Pattern>();
    private final Map<String, Pattern> regexPatternCache =
            new ConcurrentHashMap<String, Pattern>();

    public FailureCostCalculator() {
        this(AnalyzerCostSettingsLoader.loadDefault());
    }

    FailureCostCalculator(AnalyzerCostSettings costSettings) {
        this.costSettings = costSettings == null ? AnalyzerCostSettings.defaults() : costSettings;
    }

    private static class CostComputationResult {
        private float totalCost;
        private final List<AnalyzerCostDetail> costDetails =
                new ArrayList<AnalyzerCostDetail>();

        void addCost(String itemName, int count, float unitCost) {
            if (count <= 0) {
                return;
            }

            float totalItemCost = count * unitCost;
            totalCost += totalItemCost;
            costDetails.add(new AnalyzerCostDetail(itemName, count, unitCost, totalItemCost));
        }
    }

    @Override
    public void analyzeAfterExecution(AnalyzerReport report) {
        for (AnalyzerFailure failure : report.getFailures()) {
            CostComputationResult result =
                    calculateCostByType(failure.getStatementType(), failure.getSql());
            failure.setEstimatedCost(result.totalCost);
            failure.clearCostDetails();
            for (AnalyzerCostDetail costDetail : result.costDetails) {
                failure.addCostDetail(costDetail);
            }
        }
    }

    private CostComputationResult calculateCostByType(String statementType, String sql) {
        CostComputationResult result = new CostComputationResult();
        if (statementType == null) {
            return result;
        }

        switch (statementType) {
            case AnalyzerStatementTypes.TYPE_DDL_TABLE:
                return calculateTable(sql);
            case AnalyzerStatementTypes.TYPE_DDL_VIEW:
            case AnalyzerStatementTypes.TYPE_DDL_VIEW_CREATE:
            case AnalyzerStatementTypes.TYPE_DDL_VIEW_ALTER:
                return calculateView(sql);
            case AnalyzerStatementTypes.TYPE_DDL_INDEX:
                return calculateIndex(sql);
            case AnalyzerStatementTypes.TYPE_DDL_SEQUENCE:
                return calculateSequence();
            case AnalyzerStatementTypes.TYPE_DDL_SYNONYM:
                return calculateSynonym();
            case AnalyzerStatementTypes.TYPE_DDL_FUNC_HEADER:
            case AnalyzerStatementTypes.TYPE_DDL_FUNC_BODY:
                return calculateFunction(sql);
            case AnalyzerStatementTypes.TYPE_DDL_PROC_HEADER:
            case AnalyzerStatementTypes.TYPE_DDL_PROC_BODY:
                return calculateProcedure(sql);
            case AnalyzerStatementTypes.TYPE_DDL_GRANT:
                return calculateGrant(sql);
            case AnalyzerStatementTypes.TYPE_DDL_PK:
                return calculatePk();
            case AnalyzerStatementTypes.TYPE_DDL_FK:
                return calculateFk(sql);
            case AnalyzerStatementTypes.TYPE_DDL_TRIGGER:
                return calculateTrigger();
            case "SELECT":
            case "INSERT":
            case "UPDATE":
            case "DELETE":
            case AnalyzerStatementTypes.TYPE_STATIC_SQL:
                return calculateDml(sql);
            default:
                return result;
        }
    }

    private CostComputationResult calculateTable(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base table DDL", 1, baseCost(AnalyzerCostSettings.BASE_TABLE));
        result.addCost(
                "CHECK constraint",
                countCheckConstraints(upperNormalizedSql),
                heuristicCost(AnalyzerCostSettings.HEURISTIC_TABLE_CHECK_CONSTRAINT));
        result.addCost(
                "ENCRYPT keyword",
                checkKeyword(upperNormalizedSql, "ENCRYPT"),
                heuristicCost(AnalyzerCostSettings.HEURISTIC_TABLE_ENCRYPT_KEYWORD));
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getUncoveredScoreMap(),
                "Unsupported keyword");
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getOraFunctionWeightMap(),
                "Oracle function");
        appendLengthCost(result, normalizedSql);
        return result;
    }

    private CostComputationResult calculateView(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base view DDL", 1, baseCost(AnalyzerCostSettings.BASE_VIEW));
        result.addCost(
                "Subquery detected",
                hasSubquery(upperNormalizedSql) ? 1 : 0,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_VIEW_SUBQUERY));
        result.addCost(
                "JOIN detected",
                hasJoin(upperNormalizedSql) ? 1 : 0,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_VIEW_JOIN));
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getUncoveredScoreMap(),
                "Unsupported keyword");
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getOraFunctionWeightMap(),
                "Oracle function");
        appendLengthCost(result, normalizedSql);
        return result;
    }

    private CostComputationResult calculateIndex(String sql) {
        String upperNormalizedSql = normalizeQuery(sql).toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base index DDL", 1, baseCost(AnalyzerCostSettings.BASE_INDEX));
        result.addCost(
                "Function-based index",
                isFunctionBasedIndex(upperNormalizedSql) ? 1 : 0,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_INDEX_FUNCTION_BASED));
        result.addCost(
                "REVERSE keyword",
                checkKeyword(upperNormalizedSql, "REVERSE"),
                heuristicCost(AnalyzerCostSettings.HEURISTIC_INDEX_REVERSE_KEYWORD));
        return result;
    }

    private CostComputationResult calculateSequence() {
        CostComputationResult result = new CostComputationResult();
        result.addCost("Base sequence DDL", 1, baseCost(AnalyzerCostSettings.BASE_SEQUENCE));
        return result;
    }

    private CostComputationResult calculateSynonym() {
        CostComputationResult result = new CostComputationResult();
        result.addCost("Base synonym DDL", 1, baseCost(AnalyzerCostSettings.BASE_SYNONYM));
        return result;
    }

    private CostComputationResult calculateFunction(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base function DDL", 1, baseCost(AnalyzerCostSettings.BASE_FUNCTION));
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getUncoveredScoreMap(),
                "Unsupported keyword");
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getOraFunctionWeightMap(),
                "Oracle function");
        appendLengthCost(result, normalizedSql);
        return result;
    }

    private CostComputationResult calculateProcedure(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base procedure DDL", 1, baseCost(AnalyzerCostSettings.BASE_PROCEDURE));
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getUncoveredScoreMap(),
                "Unsupported keyword");
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getOraFunctionWeightMap(),
                "Oracle function");
        appendLengthCost(result, normalizedSql);
        return result;
    }

    private CostComputationResult calculateDml(String sql) {
        String normalizedSql = normalizeQuery(sql);
        String upperNormalizedSql = normalizedSql.toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base DML", 1, baseCost(AnalyzerCostSettings.BASE_DML));
        result.addCost(
                "JOIN detected",
                hasJoin(upperNormalizedSql) ? 1 : 0,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_DML_JOIN));
        result.addCost(
                "Subquery detected",
                hasSubquery(upperNormalizedSql) ? 1 : 0,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_DML_SUBQUERY));
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getUncoveredScoreMap(),
                "Unsupported keyword");
        appendKeywordCosts(
                result,
                upperNormalizedSql,
                costSettings.getOraFunctionWeightMap(),
                "Oracle function");
        appendLengthCost(result, normalizedSql);
        return result;
    }

    private CostComputationResult calculateGrant(String sql) {
        String upperNormalizedSql = normalizeQuery(sql).toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base grant DDL", 1, baseCost(AnalyzerCostSettings.BASE_GRANT));
        result.addCost(
                "Grant privilege",
                countGrantPrivileges(upperNormalizedSql),
                heuristicCost(AnalyzerCostSettings.HEURISTIC_GRANT_PRIVILEGE));
        result.addCost(
                "Missing ON clause",
                hasKeyword(upperNormalizedSql, "ON") ? 0 : 1,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_GRANT_MISSING_ON));
        result.addCost(
                "Missing TO clause",
                hasKeyword(upperNormalizedSql, "TO") ? 0 : 1,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_GRANT_MISSING_TO));
        return result;
    }

    private CostComputationResult calculatePk() {
        CostComputationResult result = new CostComputationResult();
        result.addCost("Base primary key DDL", 1, baseCost(AnalyzerCostSettings.BASE_PK));
        return result;
    }

    private CostComputationResult calculateFk(String sql) {
        String upperNormalizedSql = normalizeQuery(sql).toUpperCase(Locale.ENGLISH);

        CostComputationResult result = new CostComputationResult();
        result.addCost("Base foreign key DDL", 1, baseCost(AnalyzerCostSettings.BASE_FK));
        result.addCost(
                "ON DELETE clause",
                hasKeyword(upperNormalizedSql, "ON DELETE") ? 1 : 0,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_FK_ON_DELETE));
        return result;
    }

    private CostComputationResult calculateTrigger() {
        CostComputationResult result = new CostComputationResult();
        result.addCost("Base trigger DDL", 1, baseCost(AnalyzerCostSettings.BASE_TRIGGER));
        return result;
    }

    private void appendKeywordCosts(
            CostComputationResult result,
            String upperSql,
            Map<String, Float> keywordCostMap,
            String labelPrefix) {
        for (Map.Entry<String, Float> entry : keywordCostMap.entrySet()) {
            String keyword = entry.getKey();
            int occurrenceCount = checkKeyword(upperSql, keyword.toUpperCase(Locale.ENGLISH));
            result.addCost(labelPrefix + ": " + keyword, occurrenceCount, entry.getValue());
        }
    }

    private void appendLengthCost(CostComputationResult result, String normalizedSql) {
        int length = normalizedSql.length();
        if (length <= 199) {
            return;
        }
        if (length <= 499) {
            result.addCost(
                    "SQL length 200-499 chars (len=" + length + ")",
                    1,
                    heuristicCost(AnalyzerCostSettings.HEURISTIC_LENGTH_200_499));
            return;
        }
        if (length <= 999) {
            result.addCost(
                    "SQL length 500-999 chars (len=" + length + ")",
                    1,
                    heuristicCost(AnalyzerCostSettings.HEURISTIC_LENGTH_500_999));
            return;
        }
        result.addCost(
                "SQL length 1000+ chars (len=" + length + ")",
                1,
                heuristicCost(AnalyzerCostSettings.HEURISTIC_LENGTH_1000_PLUS));
    }

    private float baseCost(String key) {
        return costSettings.getBaseCost(key);
    }

    private float heuristicCost(String key) {
        return costSettings.getHeuristicCost(key);
    }

    /**
     * Counts CHECK constraints that should contribute to migration cost.
     * Simple nullability checks such as "IS NOT NULL" are ignored because they
     * behave more like column-nullability metadata than complex business rules.
     */
    private int countCheckConstraints(String upperNormalizedSql) {
        int count = 0;
        int fromIndex = 0;

        while ((fromIndex = findKeywordIndex(upperNormalizedSql, "CHECK", fromIndex)) >= 0) {
            int nextOpenParenIndex = upperNormalizedSql.indexOf("(", fromIndex);
            if (nextOpenParenIndex < 0) {
                count++;
                fromIndex += "CHECK".length();
                continue;
            }

            int nextCloseParenIndex =
                    findMatchingCloseParen(upperNormalizedSql, nextOpenParenIndex);
            if (nextCloseParenIndex < 0) {
                count++;
                fromIndex += "CHECK".length();
                continue;
            }

            String condition = upperNormalizedSql.substring(nextOpenParenIndex, nextCloseParenIndex + 1);
            if (!hasKeyword(condition, "IS NOT NULL")) {
                count++;
            }

            fromIndex = nextCloseParenIndex + 1;
        }

        return count;
    }

    /**
     * Finds the closing parenthesis paired with the given opening parenthesis.
     * Parentheses inside quoted SQL text do not affect the nesting depth.
     */
    private int findMatchingCloseParen(String text, int openParenIndex) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = openParenIndex; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inSingleQuote) {
                if (ch == '\'' && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    i++;
                } else if (ch == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                if (ch == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    i++;
                } else if (ch == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (ch == '\'') {
                inSingleQuote = true;
            } else if (ch == '"') {
                inDoubleQuote = true;
            } else if (ch == '(') {
                depth++;
            } else if (ch == ')' && --depth == 0) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Counts how many privileges appear between GRANT and ON.
     * Example: "GRANT SELECT, INSERT ON T1 TO U1" returns 2.
     */
    private int countGrantPrivileges(String upperNormalizedSql) {
        int onIndex = findKeywordIndex(upperNormalizedSql, "ON");
        if (!upperNormalizedSql.startsWith("GRANT ") || onIndex < 0) {
            return 0;
        }

        String privilegeSection = upperNormalizedSql.substring("GRANT ".length(), onIndex).trim();
        if (privilegeSection.isEmpty()) {
            return 0;
        }

        if (privilegeSection.contains(",")) {
            return GRANT_PRIVILEGE_SEPARATOR_PATTERN.split(privilegeSection).length;
        }

        return 1;
    }

    private boolean hasJoin(String upperNormalizedSql) {
        return hasKeyword(upperNormalizedSql, "JOIN");
    }

    private boolean hasSubquery(String upperNormalizedSql) {
        return hasPattern(upperNormalizedSql, "\\(\\s*SELECT\\b");
    }

    /**
     * Detects a function-based index by locating the ON clause and then checking
     * whether the indexed column list contains nested parentheses such as
     * "UPPER(NAME)".
     */
    private boolean isFunctionBasedIndex(String upperNormalizedSql) {
        int onIndex = findKeywordIndex(upperNormalizedSql, "ON");
        int openParenIndex = upperNormalizedSql.indexOf("(", onIndex);
        int closeParenIndex = upperNormalizedSql.lastIndexOf(")");
        if (onIndex < 0 || openParenIndex < 0) {
            return false;
        }
        if (closeParenIndex <= openParenIndex) {
            return false;
        }

        String indexColumns = upperNormalizedSql.substring(openParenIndex + 1, closeParenIndex);
        return indexColumns.contains("(");
    }

    /**
     * Counts how many times a keyword or multi-word keyword appears in the text.
     * The generated regex enforces token boundaries, so "ON" is not counted
     * inside "ONLY".
     */
    private int checkKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }

        return countPatternMatches(text, keywordPattern(keyword));
    }

    /** Convenience wrapper for boolean-style keyword checks. */
    private boolean hasKeyword(String text, String keyword) {
        return checkKeyword(text, keyword) > 0;
    }

    /** Finds the first keyword occurrence starting from the beginning of the text. */
    private int findKeywordIndex(String text, String keyword) {
        return findKeywordIndex(text, keyword, 0);
    }

    /**
     * Finds the first keyword occurrence at or after the given index.
     * This is mainly used when we need a structural anchor such as the ON clause
     * in "CREATE INDEX ... ON ...".
     */
    private int findKeywordIndex(String text, String keyword, int fromIndex) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return -1;
        }

        Matcher matcher = keywordPattern(keyword).matcher(text);
        return matcher.find(Math.max(fromIndex, 0)) ? matcher.start() : -1;
    }

    /** Generic regex-based existence check for non-keyword structural patterns. */
    private boolean hasPattern(String text, String regex) {
        if (text == null || regex == null || regex.isEmpty()) {
            return false;
        }
        return regexPattern(regex).matcher(text).find();
    }

    /** Counts regex matches for patterns that are easier to express directly. */
    private int countPatternMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private Pattern keywordPattern(String keyword) {
        return keywordPatternCache.computeIfAbsent(keyword, this::compileKeywordPattern);
    }

    private Pattern compileKeywordPattern(String keyword) {
        return Pattern.compile(buildKeywordPattern(keyword));
    }

    private Pattern regexPattern(String regex) {
        return regexPatternCache.computeIfAbsent(regex, Pattern::compile);
    }

    /**
     * Builds a regex for a keyword or multi-word keyword with token boundaries.
     * Internal whitespace becomes "\\s+" so the same keyword still matches when
     * the SQL uses spaces, tabs, or line breaks between words.
     */
    private String buildKeywordPattern(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        StringBuilder pattern = new StringBuilder();
        if (startsWithWordChar(normalizedKeyword)) {
            pattern.append("(?<![A-Z0-9_])");
        }

        pattern.append(buildLiteralKeywordPattern(normalizedKeyword));

        if (endsWithWordChar(normalizedKeyword) && !normalizedKeyword.endsWith("_")) {
            pattern.append("(?![A-Z0-9_])");
        }

        return pattern.toString();
    }

    /** Normalizes the input keyword to a stable single-space representation. */
    private String normalizeKeyword(String keyword) {
        return KEYWORD_WHITESPACE_PATTERN.matcher(keyword.trim()).replaceAll(" ");
    }

    /**
     * Converts a normalized keyword into a regex-safe literal body.
     * Example: "ON DELETE" becomes "ON\\s+DELETE".
     */
    private String buildLiteralKeywordPattern(String keyword) {
        String[] parts = KEYWORD_WHITESPACE_PATTERN.split(keyword);
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                pattern.append("\\s+");
            }
            pattern.append(Pattern.quote(parts[i]));
        }
        return pattern.toString();
    }

    /** Checks whether the first character participates in token-boundary rules. */
    private boolean startsWithWordChar(String keyword) {
        return !keyword.isEmpty() && isWordChar(keyword.charAt(0));
    }

    /** Checks whether the last character participates in token-boundary rules. */
    private boolean endsWithWordChar(String keyword) {
        return !keyword.isEmpty() && isWordChar(keyword.charAt(keyword.length() - 1));
    }

    /** Defines what this class treats as a word character inside SQL tokens. */
    private boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    /**
     * Collapses formatting differences so the later checks do not depend on
     * tabs, newlines, or repeated spaces in the original SQL text.
     */
    private String normalizeQuery(String sql) {
        if (sql == null) {
            return "";
        }

        String normalized = QUERY_FORMATTING_PATTERN.matcher(sql.trim()).replaceAll(" ");
        return MULTIPLE_SPACES_PATTERN.matcher(normalized).replaceAll(" ");
    }

}
