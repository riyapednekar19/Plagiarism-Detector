package com.example.demo;

public class Citation {
    private String title;
    private String url;
    private String domain;
    private String snippet;

    public Citation() {}

    public Citation(String title, String url, String domain, String snippet) {
        this.title = title;
        this.url = url;
        this.domain = domain;
        this.snippet = snippet;
    }

    // Getters & Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
}