package com.example.demo;

import java.util.*;

public class SimilarityEngine {

    // 1. Calculate Normalized Term Frequency (TF)
    private static Map<String, Double> computeTF(String[] words) {
        Map<String, Double> tfMap = new HashMap<>();
        double totalWords = words.length;

        if (totalWords == 0) return tfMap;

        for (String word : words) {
            word = word.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            if (!word.isEmpty()) {
                tfMap.put(word, tfMap.getOrDefault(word, 0.0) + 1.0);
            }
        }

        // Normalize TF score
        for (String key : tfMap.keySet()) {
            tfMap.put(key, tfMap.get(key) / totalWords);
        }
        return tfMap;
    }

    // 2. Compute Cosine Similarity using TF-IDF Weights
    public static double calculateCosineTFIDF(String text1, String text2) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            return 0.0;
        }

        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");

        Map<String, Double> tf1 = computeTF(words1);
        Map<String, Double> tf2 = computeTF(words2);

        // Build unique vocabulary set from both input texts
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(tf1.keySet());
        vocabulary.addAll(tf2.keySet());

        // Calculate Inverse Document Frequency (IDF)
        Map<String, Double> idfMap = new HashMap<>();
        double totalDocs = 2.0;

        for (String term : vocabulary) {
            double count = 0;
            if (tf1.containsKey(term)) count++;
            if (tf2.containsKey(term)) count++;

            // Smoothed IDF formula to prevent division by zero
            double idf = Math.log((totalDocs + 1) / (count + 1)) + 1.0;
            idfMap.put(term, idf);
        }

        // Vector Dot Product and Magnitudes
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String term : vocabulary) {
            double tfidf1 = tf1.getOrDefault(term, 0.0) * idfMap.get(term);
            double tfidf2 = tf2.getOrDefault(term, 0.0) * idfMap.get(term);

            dotProduct += tfidf1 * tfidf2;
            normA += Math.pow(tfidf1, 2);
            normB += Math.pow(tfidf2, 2);
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;

        // Cosine Similarity Formula: (A · B) / (||A|| * ||B||)
        double score = (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))) * 100.0;
        return Math.round(score * 100.0) / 100.0; // Rounded to 2 decimal places
    }
}