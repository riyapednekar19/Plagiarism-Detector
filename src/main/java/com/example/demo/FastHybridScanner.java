package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FastHybridScanner {

    @Autowired
    private HybridScoringEngine hybridScoringEngine;

    public double calculateHybridScore(String sourceText, String suspectText) {
        // Compute raw Jaccard and Cosine scores
        double jaccardScore = calculateJaccard(sourceText, suspectText);
        double cosineScore = calculateCosine(sourceText, suspectText);

        // Pass through Dynamic Adaptive Weighting
        return hybridScoringEngine.calculateDynamicHybridScore(jaccardScore, cosineScore);
    }

    private double calculateJaccard(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) return 0.0;

        java.util.Set<String> words1 = new java.util.HashSet<>(java.util.Arrays.asList(text1.toLowerCase().split("\\s+")));
        java.util.Set<String> words2 = new java.util.HashSet<>(java.util.Arrays.asList(text2.toLowerCase().split("\\s+")));

        java.util.Set<String> intersection = new java.util.HashSet<>(words1);
        intersection.retainAll(words2);

        java.util.Set<String> union = new java.util.HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double calculateCosine(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) return 0.0;

        java.util.Map<String, Integer> freq1 = getTermFrequencies(text1);
        java.util.Map<String, Integer> freq2 = getTermFrequencies(text2);

        java.util.Set<String> allWords = new java.util.HashSet<>(freq1.keySet());
        allWords.addAll(freq2.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String word : allWords) {
            int count1 = freq1.getOrDefault(word, 0);
            int count2 = freq2.getOrDefault(word, 0);

            dotProduct += count1 * count2;
            normA += count1 * count1;
            normB += count2 * count2;
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private java.util.Map<String, Integer> getTermFrequencies(String text) {
        java.util.Map<String, Integer> freqMap = new java.util.HashMap<>();
        for (String word : text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+")) {
            if (!word.trim().isEmpty()) {
                freqMap.put(word, freqMap.getOrDefault(word, 0) + 1);
            }
        }
        return freqMap;
    }
}
