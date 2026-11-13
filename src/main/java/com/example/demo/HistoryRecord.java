package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "history_records")
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // Traces who performed the check

    private LocalDateTime timestamp;

    @Column(name = "document_name")
    private String documentName; // Shows "Text Scan" or the uploaded filename

    @Column(name = "match_percentage")
    private String matchPercentage; // e.g., "15% Plagiarized" or "0% Clean"

    private String status; // e.g., "Flagged" or "Verified"

    @Lob
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent; // Stores extracted text content for backend fingerprint comparisons

    // 🔗 ManyToOne Mapping to fix Hibernate 'mappedBy = "user"' error
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Default Constructor (Required by JPA)
    public HistoryRecord() {
    }

    // Parametrized Constructor (Without text content)
    public HistoryRecord(String username, LocalDateTime timestamp, String documentName, String matchPercentage, String status) {
        this.username = username;
        this.timestamp = timestamp;
        this.documentName = documentName;
        this.matchPercentage = matchPercentage;
        this.status = status;
    }

    // Full Parametrized Constructor (With text content)
    public HistoryRecord(String username, LocalDateTime timestamp, String documentName, String matchPercentage, String status, String textContent) {
        this.username = username;
        this.timestamp = timestamp;
        this.documentName = documentName;
        this.matchPercentage = matchPercentage;
        this.status = status;
        this.textContent = textContent;
    }

    // Full Parametrized Constructor (With User reference)
    public HistoryRecord(User user, LocalDateTime timestamp, String documentName, String matchPercentage, String status, String textContent) {
        this.user = user;
        this.username = (user != null) ? user.getUsername() : "Guest";
        this.timestamp = timestamp;
        this.documentName = documentName;
        this.matchPercentage = matchPercentage;
        this.status = status;
        this.textContent = textContent;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(String matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}