package com.example.demo;

import java.util.List;
import java.util.Map;

public class PlagiarismReport {

    // --- Executive Header & Metadata ---
    private String reportId;
    private String timestamp;
    private String documentTitle;
    private int wordCount;
    private int characterCount;

    // --- Similarity Scores ---
    private double overallSimilarityPercentage;
    private double lexicalJaccardScore;
    private double semanticCosineScore;
    private String scanStageExecuted; // STAGE_1_FAST_SCAN or STAGE_2_DEEP_SBERT_SCAN

    // --- Match Categorization & Breakdown ---
    private double verbatimMatchPercentage;
    private double paraphrasedMatchPercentage;

    // --- Annotated Text & Source List ---
    private String annotatedTextHtml; // HTML text with color-coded <mark> tags
    private List<MatchedSource> matchedSources;

    // --- Exclusion Summary ---
    private Map<String, Boolean> exclusionsApplied; // e.g., {"bibliographyExcluded": true, "quotesExcluded": true}

    public PlagiarismReport() {
    }

    public PlagiarismReport(String reportId, String timestamp, String documentTitle, int wordCount,
                            int characterCount, double overallSimilarityPercentage, double lexicalJaccardScore,
                            double semanticCosineScore, String scanStageExecuted, double verbatimMatchPercentage,
                            double paraphrasedMatchPercentage, String annotatedTextHtml,
                            List<MatchedSource> matchedSources, Map<String, Boolean> exclusionsApplied) {
        this.reportId = reportId;
        this.timestamp = timestamp;
        this.documentTitle = documentTitle;
        this.wordCount = wordCount;
        this.characterCount = characterCount;
        this.overallSimilarityPercentage = overallSimilarityPercentage;
        this.lexicalJaccardScore = lexicalJaccardScore;
        this.semanticCosineScore = semanticCosineScore;
        this.scanStageExecuted = scanStageExecuted;
        this.verbatimMatchPercentage = verbatimMatchPercentage;
        this.paraphrasedMatchPercentage = paraphrasedMatchPercentage;
        this.annotatedTextHtml = annotatedTextHtml;
        this.matchedSources = matchedSources;
        this.exclusionsApplied = exclusionsApplied;
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getCharacterCount() { return characterCount; }
    public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }

    public double getOverallSimilarityPercentage() { return overallSimilarityPercentage; }
    public void setOverallSimilarityPercentage(double overallSimilarityPercentage) { this.overallSimilarityPercentage = overallSimilarityPercentage; }

    public double getLexicalJaccardScore() { return lexicalJaccardScore; }
    public void setLexicalJaccardScore(double lexicalJaccardScore) { this.lexicalJaccardScore = lexicalJaccardScore; }

    public double getSemanticCosineScore() { return semanticCosineScore; }
    public void setSemanticCosineScore(double semanticCosineScore) { this.semanticCosineScore = semanticCosineScore; }

    public String getScanStageExecuted() { return scanStageExecuted; }
    public void setScanStageExecuted(String scanStageExecuted) { this.scanStageExecuted = scanStageExecuted; }

    public double getVerbatimMatchPercentage() { return verbatimMatchPercentage; }
    public void setVerbatimMatchPercentage(double verbatimMatchPercentage) { this.verbatimMatchPercentage = verbatimMatchPercentage; }

    public double getParaphrasedMatchPercentage() { return paraphrasedMatchPercentage; }
    public void setParaphrasedMatchPercentage(double paraphrasedMatchPercentage) { this.paraphrasedMatchPercentage = paraphrasedMatchPercentage; }

    public String getAnnotatedTextHtml() { return annotatedTextHtml; }
    public void setAnnotatedTextHtml(String annotatedTextHtml) { this.annotatedTextHtml = annotatedTextHtml; }

    public List<MatchedSource> getMatchedSources() { return matchedSources; }
    public void setMatchedSources(List<MatchedSource> matchedSources) { this.matchedSources = matchedSources; }

    public Map<String, Boolean> getExclusionsApplied() { return exclusionsApplied; }
    public void setExclusionsApplied(Map<String, Boolean> exclusionsApplied) { this.exclusionsApplied = exclusionsApplied; }
}