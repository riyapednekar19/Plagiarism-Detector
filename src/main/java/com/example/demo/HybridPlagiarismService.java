package com.example.demo;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.WebColors;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public class HybridPlagiarismService {

    private Map<String, Double> computeTF(String[] words) {
        Map<String, Double> tfMap = new HashMap<>();
        double totalWords = words.length;

        for (String word : words) {
            tfMap.put(word, tfMap.getOrDefault(word, 0.0) + 1.0);
        }
        for (String word : tfMap.keySet()) {
            tfMap.put(word, tfMap.get(word) / totalWords);
        }
        return tfMap;
    }

    public double calculateCosineTFIDF(String text1, String text2) {
        String[] words1 = text1.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+");
        String[] words2 = text2.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+");

        if (words1.length == 0 || words2.length == 0) return 0.0;

        Map<String, Double> tf1 = computeTF(words1);
        Map<String, Double> tf2 = computeTF(words2);

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(tf1.keySet());
        vocabulary.addAll(tf2.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String word : vocabulary) {
            double v1 = tf1.getOrDefault(word, 0.0);
            double v2 = tf2.getOrDefault(word, 0.0);

            dotProduct += v1 * v2;
            normA += v1 * v1;
            normB += v2 * v2;
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return similarity * 100.0;
    }

    public double calculateJaccard(String text1, String text2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return ((double) intersection.size() / union.size()) * 100.0;
    }

    public VerificationResponse analyze(String submission, String source) {
        String cleanSubmission = cleanText(submission);
        String cleanSource = cleanText(source);

        double jaccardScore = calculateJaccard(cleanSubmission, cleanSource);
        double cosineScore = calculateCosineTFIDF(cleanSubmission, cleanSource);

        double hybridScore = (0.30 * jaccardScore) + (0.70 * cosineScore);
        double finalScore = Math.round(hybridScore * 100.0) / 100.0;

        String status = finalScore > 20.0 ? "Flagged" : "Verified";

        return new VerificationResponse(
                finalScore,
                Math.round(jaccardScore * 100.0) / 100.0,
                Math.round(cosineScore * 100.0) / 100.0,
                status,
                cleanSubmission,
                cleanSource
        );
    }

    public ByteArrayInputStream generatePdfReport(VerificationResponse response) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, WebColors.getRGBColor("#1A2530"));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, WebColors.getRGBColor("#2C3E50"));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, WebColors.getRGBColor("#4A5568"));
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);

            // 1. Report Header
            Paragraph title = new Paragraph("Plagiarism Audit Report", titleFont);
            title.setSpacingAfter(10f);
            document.add(title);

            // 2. Similarity Score Dashboard
            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(12f);

            addDashboardCell(summaryTable, "Overall Similarity", response.getFinalScore() + "%", sectionFont, subFont);
            addDashboardCell(summaryTable, "Lexical Overlap", response.getJaccardScore() + "%", sectionFont, subFont);
            addDashboardCell(summaryTable, "TF-IDF Vector", response.getCosineScore() + "%", sectionFont, subFont);
            document.add(summaryTable);

            // 3. Section: Submitted Document Content
            Paragraph sec3Header = new Paragraph("3. Submitted Document Content", sectionFont);
            sec3Header.setSpacingAfter(4f);
            document.add(sec3Header);

            PdfPTable subBox = new PdfPTable(1);
            subBox.setWidthPercentage(100);
            subBox.setSpacingAfter(12f);

            PdfPCell subCell = new PdfPCell();
            subCell.setBackgroundColor(WebColors.getRGBColor("#FAFAFA"));
            subCell.setPadding(8f);
            subCell.addElement(PdfHighlightUtil.buildHighlightedParagraph(response.getSource(), response.getSubmission()));
            subBox.addCell(subCell);
            document.add(subBox);

            // 4. Section: Matched Reference / Source Content
            Paragraph sec4Header = new Paragraph("4. Matched Reference / Source Content", sectionFont);
            sec4Header.setSpacingAfter(4f);
            document.add(sec4Header);

            PdfPTable srcBox = new PdfPTable(1);
            srcBox.setWidthPercentage(100);
            srcBox.setSpacingAfter(12f);

            PdfPCell srcCell = new PdfPCell();
            srcCell.setBackgroundColor(WebColors.getRGBColor("#FFFDE7"));
            srcCell.setPadding(8f);
            srcCell.addElement(new Paragraph(response.getSource(), textFont));
            srcBox.addCell(srcCell);
            document.add(srcBox);

            // 5. Section: Exact Side-by-Side Verification Mapping (Matching Web UI Layout)
            Paragraph sec5Header = new Paragraph("5. Side-by-Side Verification Mapping", sectionFont);
            sec5Header.setSpacingAfter(4f);
            document.add(sec5Header);

            PdfPTable sideBySideTable = new PdfPTable(2);
            sideBySideTable.setWidthPercentage(100);
            sideBySideTable.setWidths(new float[]{1f, 1f});
            sideBySideTable.setSpacingAfter(12f);

            PdfPCell col1Header = new PdfPCell(new Paragraph("SUBMITTED CORE", subFont));
            col1Header.setBackgroundColor(WebColors.getRGBColor("#EDF2F7"));
            col1Header.setPadding(6f);

            PdfPCell col2Header = new PdfPCell(new Paragraph("IDENTIFIED MATCH RESOURCE", subFont));
            col2Header.setBackgroundColor(WebColors.getRGBColor("#EDF2F7"));
            col2Header.setPadding(6f);

            sideBySideTable.addCell(col1Header);
            sideBySideTable.addCell(col2Header);

            // Left Column: Submitted Text with Highlights
            PdfPCell col1Cell = new PdfPCell();
            col1Cell.setBackgroundColor(Color.WHITE);
            col1Cell.setPadding(8f);
            col1Cell.addElement(PdfHighlightUtil.buildHighlightedParagraph(response.getSource(), response.getSubmission()));

            // Right Column: Clean Source Text
            PdfPCell col2Cell = new PdfPCell();
            col2Cell.setBackgroundColor(Color.WHITE);
            col2Cell.setPadding(8f);
            col2Cell.addElement(new Paragraph(response.getSource(), textFont));

            sideBySideTable.addCell(col1Cell);
            sideBySideTable.addCell(col2Cell);
            document.add(sideBySideTable);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\$\\\\ge\\s*", "≥ ")
                .replaceAll("\\$\\\\le\\s*", "≤ ")
                .replaceAll("\\$", "")
                .replaceAll("\\\\\\%", "%");
    }

    private void addDashboardCell(PdfPTable table, String header, String value, Font headFont, Font bodyFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);
        cell.setBackgroundColor(WebColors.getRGBColor("#F1F5F9"));
        cell.addElement(new Paragraph(header, headFont));
        cell.addElement(new Paragraph(value, bodyFont));
        table.addCell(cell);
    }
}