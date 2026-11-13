package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class PlagiarismService {

    public VerificationResponse processCheck(String submissionText, String sourceText) {
        VerificationResponse response = new VerificationResponse();

        // Run TF-IDF Cosine Calculation
        double similarityScore = SimilarityEngine.calculateCosineTFIDF(submissionText, sourceText);

        // Populate your existing response model
        response.setFinalScore(similarityScore);

        if (similarityScore > 50) {
            response.setStatus("Plagiarism Detected");
        } else if (similarityScore > 15) {
            response.setStatus("Moderate Match");
        } else {
            response.setStatus("Verified");
        }

        return response;
    }
}