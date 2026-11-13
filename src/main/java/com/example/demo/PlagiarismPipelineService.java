package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PlagiarismPipelineService {

    @Autowired
    private FastHybridScanner fastScanner;

    @Autowired
    private SbertClientService sbertClient;

    public Map<String, Object> processPlagiarismCheck(String sourceText, String suspectText) {
        Map<String, Object> response = new HashMap<>();

        // --- STAGE 1: Fast Filter (Milliseconds) ---
        double primaryScore = fastScanner.calculateHybridScore(sourceText, suspectText);

        // If score is clearly low (<40%) or clearly high (>60%), return immediately
        if (primaryScore < 0.40 || primaryScore > 0.60) {
            response.put("score", primaryScore);
            response.put("scanStage", "STAGE_1_FAST_SCAN");
            response.put("statusMessage", "Clear Match / Non-Plagiarized");
            return response;
        }

        // --- STAGE 2: Targeted Deep Scan (Conditional SBERT) ---
        // Triggered only for ambiguous paraphrasing cases (40% - 60%)
        double sbertScore = sbertClient.fetchDeepSemanticSimilarity(sourceText, suspectText);

        // Combine primary score with deep semantic score
        double finalPrecisionScore = (primaryScore * 0.40) + (sbertScore * 0.60);

        response.put("score", finalPrecisionScore);
        response.put("scanStage", "STAGE_2_DEEP_SBERT_SCAN");
        response.put("statusMessage", "Contextual Ambiguity Resolved");

        return response;
    }
}