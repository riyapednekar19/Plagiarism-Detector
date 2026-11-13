package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class HybridScoringEngine {

    /**
     * Calculates an adaptive hybrid similarity score combining Jaccard (lexical)
     * and TF-IDF Cosine (vector) metrics based on input characteristics.
     *
     * @param rawJaccard Raw set-based Jaccard similarity score (0.0 to 1.0)
     * @param rawCosine  Raw TF-IDF vector Cosine similarity score (0.0 to 1.0)
     * @return Dynamically weighted final similarity score (0.0 to 1.0)
     */
    public double calculateDynamicHybridScore(double rawJaccard, double rawCosine) {
        double jaccardWeight;
        double cosineWeight;

        // Dynamic Rule 1: High verbatim copy-pasting (>= 80%)
        // Shift primary weight to literal set-matching to capture exact matches
        if (rawJaccard >= 0.80) {
            jaccardWeight = 0.80;
            cosineWeight = 0.20;
        }
        // Dynamic Rule 2: Low lexical overlap (< 20%) but high vector similarity (>= 70%)
        // Shift weight to vector space orientation to handle heavy paraphrasing
        else if (rawJaccard < 0.20 && rawCosine >= 0.70) {
            jaccardWeight = 0.10;
            cosineWeight = 0.90;
        }
        // Default Rule: Standard balanced 30% Lexical / 70% Vector track
        else {
            jaccardWeight = 0.30;
            cosineWeight = 0.70;
        }

        // Compute adaptive weighted sum
        return (rawJaccard * jaccardWeight) + (rawCosine * cosineWeight);
    }
}