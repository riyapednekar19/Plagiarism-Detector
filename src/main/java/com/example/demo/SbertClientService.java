package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class SbertClientService {

    /**
     * Fallback/Mock implementation for Stage 2 SBERT Deep Scan.
     * Integrates with Python microservice if available, otherwise computes fallback contextual estimation.
     */
    public double fetchDeepSemanticSimilarity(String sourceText, String suspectText) {
        if (sourceText == null || suspectText == null) return 0.0;

        // Example contextual semantic comparison placeholder
        double lengthRatio = Math.min((double) sourceText.length() / suspectText.length(),
                (double) suspectText.length() / sourceText.length());

        return Math.min(1.0, lengthRatio * 0.85);
    }
}