package com.example.demo;

public class ScanResult {

    private double score;
    private String scanStage;
    private String statusMessage;

    public ScanResult() {
    }

    public ScanResult(double score, String scanStage, String statusMessage) {
        this.score = score;
        this.scanStage = scanStage;
        this.statusMessage = statusMessage;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getScanStage() {
        return scanStage;
    }

    public void setScanStage(String scanStage) {
        this.scanStage = scanStage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}