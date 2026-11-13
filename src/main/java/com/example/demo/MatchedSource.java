package com.example.demo;

public class MatchedSource {

    private int sourceId;
    private String sourceName;
    private String sourceUrl;
    private double matchPercentage;

    public MatchedSource() {
    }

    public MatchedSource(int sourceId, String sourceName, String sourceUrl, double matchPercentage) {
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.matchPercentage = matchPercentage;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public double getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(double matchPercentage) {
        this.matchPercentage = matchPercentage;
    }
}