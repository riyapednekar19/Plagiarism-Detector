package com.example.demo;

public class VerificationResponse {
    private double finalScore;
    private double jaccardScore;
    private double cosineScore;
    private String status;
    private String submission;
    private String source;

    public VerificationResponse() {}

    public VerificationResponse(double finalScore, double jaccardScore, double cosineScore,
                                String status, String submission, String source) {
        this.finalScore = finalScore;
        this.jaccardScore = jaccardScore;
        this.cosineScore = cosineScore;
        this.status = status;
        this.submission = submission;
        this.source = source;
    }

    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

    public double getJaccardScore() { return jaccardScore; }
    public void setJaccardScore(double jaccardScore) { this.jaccardScore = jaccardScore; }

    public double getCosineScore() { return cosineScore; }
    public void setCosineScore(double cosineScore) { this.cosineScore = cosineScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmission() { return submission; }
    public void setSubmission(String submission) { this.submission = submission; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}