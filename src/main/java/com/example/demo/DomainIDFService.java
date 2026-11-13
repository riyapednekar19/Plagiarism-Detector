package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DomainIDFService {

    private final Map<String, Double> domainIdfDictionary = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadDomainCorpus() {
        // Load pre-calculated domain weights (e.g., from JSON/DB)
        domainIdfDictionary.put("polymorphism", 1.05); // Frequently occurs in CS -> Lowered weight
        domainIdfDictionary.put("encapsulation", 1.10);
        domainIdfDictionary.put("plagiarism", 2.85);    // Topic specific -> Higher weight
    }

    public double getIdfWeight(String term, int totalDocs, int docsWithTerm) {
        // If term exists in domain dictionary, return tuned weight; else fallback to standard calculation
        return domainIdfDictionary.getOrDefault(
                term.toLowerCase(),
                Math.log((double)(totalDocs + 1) / (docsWithTerm + 1)) + 1.0
        );
    }
}
