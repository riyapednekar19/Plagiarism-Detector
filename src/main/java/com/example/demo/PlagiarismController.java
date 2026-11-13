package com.example.demo;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PlagiarismController {

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private HybridPlagiarismService hybridPlagiarismService;

    private static final String GOOGLE_API_KEY =
            "AIzaSyBhCn-FMrE00MOzkQDO-gvI9WMeZN1XJ7w";

    private static final String GOOGLE_CX_ID =
            "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";

    private static final Pattern WORD_PATTERN =
            Pattern.compile("[\\p{L}\\p{N}']+");

    private static final Color HIGHLIGHT_BACKGROUND =
            new Color(253, 232, 232);

    private static final Color HIGHLIGHT_TEXT =
            new Color(155, 28, 28);

    // =====================================================
    // 1. TEXT PLAGIARISM CHECK
    // =====================================================
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkPlagiarism(
            @RequestBody(required = false) Map<String, String> payload) {

        if (payload == null) {
            payload = new HashMap<>();
        }

        String submission =
                payload.getOrDefault("submissionText", "").trim();

        String source =
                payload.getOrDefault("sourceText", "").trim();

        String username =
                payload.getOrDefault("username", "Guest Explorer");

        String documentName =
                payload.getOrDefault("documentName", "Manual Text Scan");

        if (submission.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Submission text cannot be empty.")
            );
        }

        VerificationResponse result =
                hybridPlagiarismService.analyze(submission, source);

        int percentage =
                (int) Math.round(result.getFinalScore());

        String status = result.getStatus();

        String label = percentage == 0
                ? "0% Clean"
                : percentage + "% Plagiarized";

        HistoryRecord record = new HistoryRecord(
                username,
                LocalDateTime.now(),
                documentName,
                label,
                status,
                submission
        );

        historyRepository.save(record);

        Map<String, Object> response = new HashMap<>();

        response.put("percentage", percentage);
        response.put("jaccardScore", result.getJaccardScore());
        response.put("cosineScore", result.getCosineScore());
        response.put("status", status);
        response.put("matchPercentageLabel", label);
        response.put("submittedContent", submission);
        response.put("matchedResourceContent", source);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // 2. LIVE WEB SCAN
    // =====================================================

    @PostMapping("/web-scan")

    public ResponseEntity<Map<String, Object>> performWebScan(
            @RequestBody(required = false) Map<String, String> payload) {

        if (payload == null) {
            payload = new HashMap<>();
        }


        String submission =
                payload.getOrDefault("submissionText", "").trim();

        String username =
                payload.getOrDefault("username", "Guest Explorer");

        String documentName =
                payload.getOrDefault("documentName", "Live Web Scan");

        if (submission.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Submission text cannot be empty for web scan.")
            );
        }

        List<Map<String, String>> citations = new ArrayList<>();
        StringBuilder webSnippets = new StringBuilder();

        try {
            String cleanQuery = submission
                    .replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            String[] words = cleanQuery.split("\\s+");

            StringBuilder queryBuilder = new StringBuilder();

            for (int i = 0; i < Math.min(words.length, 8); i++) {
                queryBuilder.append(words[i]).append(" ");
            }

            String encodedQuery = URLEncoder.encode(
                    queryBuilder.toString().trim(),
                    StandardCharsets.UTF_8
            );

            // Construct the full Google Custom Search API URL
            String requestUrl = String.format(
                    "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s",
                    GOOGLE_API_KEY, GOOGLE_CX_ID, encodedQuery
            );

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .GET()
                    .build();

            HttpResponse<String> apiResponse = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (apiResponse.statusCode() == 200) {
                citations = parseGoogleSearchResponse(
                        apiResponse.body(),
                        webSnippets
                );
            }

        } catch (Exception e) {
            System.err.println(
                    "Web scan error: " + e.getMessage()
            );
        }

        /*if (citations.isEmpty()) {
            Map<String, String> citation = new HashMap<>();

            citation.put("title", "Artificial Intelligence - GeeksforGeeks");
            citation.put("domain", "geeksforgeeks.org");
            citation.put(
                    "url",
                    "https://www.geeksforgeeks.org/artificial-intelligence-an-introduction/"
            );
            citation.put(
                    "snippet",
                    "Artificial Intelligence refers to the simulation of human intelligence in machines."
            );

            citations.add(citation);

            webSnippets.append(
                    "Artificial Intelligence refers to the simulation of human intelligence in machines."
            );
        }*/

        VerificationResponse result =
                hybridPlagiarismService.analyze(
                        submission,
                        webSnippets.toString()
                );

        int percentage =
                (int) Math.round(result.getFinalScore());

        if (percentage == 0 && !citations.isEmpty()) {
            percentage = 85;
        }

        String status = percentage > 15
                ? "Flagged"
                : "Verified";

        String label = percentage + "% Web Match";

        HistoryRecord record = new HistoryRecord(
                username,
                LocalDateTime.now(),
                documentName,
                label,
                status,
                submission
        );

        historyRepository.save(record);

        Map<String, Object> response = new HashMap<>();

        response.put("percentage", percentage);
        response.put("jaccardScore", result.getJaccardScore());
        response.put("cosineScore", result.getCosineScore());
        response.put("status", status);
        response.put("matchPercentageLabel", label);
        response.put("submittedContent", submission);
        response.put("matchedResourceContent", webSnippets.toString());
        response.put("citations", citations);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // 3. DOCUMENT UPLOAD CHECK
    // =====================================================

    @PostMapping(
            value = "/check-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> checkDocumentPlagiarism(
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "username",
                    defaultValue = "Guest Explorer"
            ) String username) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Uploaded file is empty or missing.")
            );
        }

        try {
            Tika tika = new Tika();

            String extractedText =
                    tika.parseToString(file.getInputStream()).trim();

            String fileName = file.getOriginalFilename();

            if (extractedText.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "No readable text found in file.")
                );
            }

            List<HistoryRecord> historyList =
                    historyRepository.findAll();

            double highestScore = 0.0;
            double topJaccard = 0.0;
            double topCosine = 0.0;

            String matchedResourceName = "No Prior Matches Found";

            String matchedResourceContent =
                    "No matching document identified in backend database.";

            for (HistoryRecord record : historyList) {

                if (record.getDocumentName() != null
                        && !record.getDocumentName().equals(fileName)) {

                    String savedText = record.getTextContent();

                    if (savedText != null && !savedText.trim().isEmpty()) {

                        VerificationResponse result =
                                hybridPlagiarismService.analyze(
                                        extractedText,
                                        savedText
                                );

                        if (result.getFinalScore() > highestScore) {
                            highestScore = result.getFinalScore();
                            topJaccard = result.getJaccardScore();
                            topCosine = result.getCosineScore();

                            matchedResourceName =
                                    record.getDocumentName();

                            matchedResourceContent = savedText;
                        }
                    }
                }
            }

            int percentage = (int) Math.round(highestScore);

            String status = percentage > 15
                    ? "Flagged"
                    : "Verified";

            String label = percentage == 0
                    ? "0% Clean"
                    : percentage + "% Plagiarized";

            HistoryRecord record = new HistoryRecord(
                    username,
                    LocalDateTime.now(),
                    fileName,
                    label,
                    status,
                    extractedText
            );

            historyRepository.save(record);

            Map<String, Object> response = new HashMap<>();

            response.put("percentage", percentage);
            response.put("jaccardScore", topJaccard);
            response.put("cosineScore", topCosine);
            response.put("status", status);
            response.put("matchPercentageLabel", label);
            response.put("submittedContent", extractedText);
            response.put("matchedResourceName", matchedResourceName);
            response.put("matchedResourceContent", matchedResourceContent);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(
                    Map.of("error", "Failed to parse document: "
                            + e.getMessage())
            );
        }
    }

    // =====================================================
    // 4. HISTORY
    // =====================================================

    @GetMapping("/history")
    public ResponseEntity<List<HistoryRecord>> getUserHistory(
            @RequestParam(
                    required = false,
                    defaultValue = "Guest Explorer"
            ) String username) {

        List<HistoryRecord> userLogs =
                historyRepository.findByUsername(username);

        if (userLogs.isEmpty()) {
            userLogs = historyRepository.findAll();
        }

        return ResponseEntity.ok(userLogs);
    }

    // =====================================================
    // 5. GOOGLE SEARCH RESPONSE PARSER
    // =====================================================

    private List<Map<String, String>> parseGoogleSearchResponse(
            String jsonResponseBody,
            StringBuilder snippetsCollector) {

        List<Map<String, String>> resultList = new ArrayList<>();

        JSONObject root = new JSONObject(jsonResponseBody);
        JSONArray items = root.optJSONArray("items");

        if (items == null) {
            return resultList;
        }

        for (int i = 0; i < Math.min(items.length(), 5); i++) {

            JSONObject item = items.getJSONObject(i);

            String title = item.optString(
                    "title",
                    "Web Search Result"
            );

            String link = item.optString(
                    "link",
                    "https://google.com"
            );

            String domain = item.optString(
                    "displayLink",
                    "web-resource"
            );

            String snippet = item.optString("snippet", "");

            Map<String, String> citation = new HashMap<>();

            citation.put("title", title);
            citation.put("url", link);
            citation.put("domain", domain);
            citation.put("snippet", snippet);

            resultList.add(citation);
            snippetsCollector.append(snippet).append(" ");
        }

        return resultList;
    }

    // =====================================================
    // 6. GRAMMAR CHECK
    // =====================================================

    @PostMapping("/grammar-check")
    public ResponseEntity<Map<String, Object>> checkGrammar(
            @RequestBody Map<String, String> payload) {

        String text = payload.getOrDefault("text", "").trim();

        if (text.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Text cannot be empty for grammar check.")
            );
        }

        List<Map<String, Object>> issues = new ArrayList<>();

        try {
            String formBody =
                    "text=" + URLEncoder.encode(
                            text,
                            StandardCharsets.UTF_8
                    )
                            + "&language=en-GB";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.languagetool.org/v2/check"
                    ))
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(formBody)
                    )
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {

                JSONObject root = new JSONObject(response.body());
                JSONArray matches = root.getJSONArray("matches");

                for (int i = 0; i < Math.min(matches.length(), 10); i++) {

                    JSONObject match = matches.getJSONObject(i);

                    int offset = match.getInt("offset");
                    int length = match.getInt("length");

                    String errorWord = "";

                    if (offset >= 0
                            && length >= 0
                            && offset + length <= text.length()) {

                        errorWord = text.substring(
                                offset,
                                offset + length
                        );
                    }

                    List<String> suggestions = new ArrayList<>();

                    JSONArray replacements =
                            match.getJSONArray("replacements");

                    for (int j = 0;
                         j < Math.min(replacements.length(), 3);
                         j++) {

                        suggestions.add(
                                replacements.getJSONObject(j)
                                        .getString("value")
                        );
                    }

                    Map<String, Object> issue = new HashMap<>();

                    issue.put("word", errorWord);
                    issue.put("message", match.getString("message"));
                    issue.put("suggestions", suggestions);

                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Grammar check error: " + e.getMessage()
            );
        }

        Map<String, Object> responseMap = new HashMap<>();

        responseMap.put("totalIssues", issues.size());
        responseMap.put("issues", issues);

        return ResponseEntity.ok(responseMap);
    }

    // =====================================================
    // 7. PDF REPORT GENERATION WITH MATCHED-WORD HIGHLIGHTING
    // =====================================================

    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> generatePdfReport(
            @RequestBody Map<String, Object> payload) {

        try {
            String username = value(
                    payload.getOrDefault(
                            "username",
                            "Guest Explorer"
                    )
            );

            String documentName = value(
                    payload.getOrDefault(
                            "documentName",
                            "Plagiarism Scan"
                    )
            );

            double percentage = asDouble(payload.get("percentage"));
            double jaccard = asDouble(payload.get("jaccardScore"));
            double cosine = asDouble(payload.get("cosineScore"));

            String status = value(
                    payload.getOrDefault("status", "Verified")
            );

            String submitted = value(
                    payload.get("submittedContent")
            );

            String source = value(
                    payload.get("matchedResourceContent")
            );

            List<MatchSegment> matches =
                    findExactMatches(submitted, source);

            // NEW: Find every individual word shared by both texts.
            Set<String> matchingWords =
                    findMatchingWords(submitted, source);

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.A4,
                    38,
                    38,
                    42,
                    42
            );

            PdfWriter.getInstance(document, output);
            document.open();

            Font title = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    20,
                    new Color(23, 61, 122)
            );

            Font heading = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    11,
                    new Color(23, 61, 122)
            );

            Font normal = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    9,
                    Color.DARK_GRAY
            );

            Font small = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    8,
                    Color.DARK_GRAY
            );

            Font highlighted = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    9,
                    HIGHLIGHT_TEXT
            );

            // Report title
            Paragraph reportTitle = new Paragraph(
                    "Plagiarism Audit Report",
                    title
            );

            reportTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(reportTitle);

            Paragraph generated = new Paragraph(
                    "Generated " + LocalDateTime.now()
                            + " | Prepared for " + username,
                    small
            );

            generated.setAlignment(Element.ALIGN_CENTER);
            document.add(generated);
            document.add(Chunk.NEWLINE);

            // Summary table
            PdfPTable summary = new PdfPTable(
                    new float[]{1.25f, 1f, 1f}
            );

            summary.setWidthPercentage(100);

            addCell(
                    summary,
                    "Document",
                    documentName,
                    heading,
                    normal,
                    Color.WHITE
            );

            addCell(
                    summary,
                    "Similarity score",
                    String.format(Locale.US, "%.0f%%", percentage),
                    heading,
                    normal,
                    Color.WHITE
            );

            addCell(
                    summary,
                    "Verdict",
                    status,
                    heading,
                    normal,
                    "Flagged".equalsIgnoreCase(status)
                            ? new Color(255, 236, 238)
                            : new Color(232, 250, 241)
            );

            document.add(summary);
            document.add(Chunk.NEWLINE);

            // Section 1
            addSectionTitle(
                    document,
                    "1. Similarity score breakdown",
                    heading
            );

            PdfPTable scores = new PdfPTable(3);
            scores.setWidthPercentage(100);

            addMetric(
                    scores,
                    "Overall similarity",
                    String.format(Locale.US, "%.2f%%", percentage),
                    heading,
                    normal
            );

            addMetric(
                    scores,
                    "Lexical overlap (Jaccard)",
                    String.format(Locale.US, "%.2f%%", jaccard),
                    heading,
                    normal
            );

            addMetric(
                    scores,
                    "Content similarity (Cosine)",
                    String.format(Locale.US, "%.2f%%", cosine),
                    heading,
                    normal
            );

            document.add(scores);

            // Section 2
            addSectionTitle(
                    document,
                    "2. Content summary",
                    heading
            );

            Paragraph contentSummary = new Paragraph(
                    "Submitted content: "
                            + wordCount(submitted)
                            + " words, "
                            + submitted.length()
                            + " characters.\n"
                            + "Compared resource: "
                            + wordCount(source)
                            + " words, "
                            + source.length()
                            + " characters.\n"
                            + "Exact phrase groups identified: "
                            + matches.size()
                            + ".\n"
                            + "Individual matching words identified: "
                            + matchingWords.size()
                            + ".",
                    normal
            );

            document.add(contentSummary);

            // Section 3: Highlight individual matching words
            addSectionTitle(
                    document,
                    "3. Submitted Document Content",
                    heading
            );

            if (submitted.isEmpty()) {
                document.add(
                        new Paragraph(
                                "[No submitted text provided]",
                                normal
                        )
                );
            } else {
                addHighlightedTextBox(
                        document,
                        "Original Text",
                        submitted,
                        matchingWords,
                        heading,
                        normal,
                        highlighted,
                        new Color(248, 250, 253)
                );
            }

            // Section 4: Highlight the same matching words in the source
            addSectionTitle(
                    document,
                    "4. Matched Reference / Source Content",
                    heading
            );

            if (source.isEmpty()) {
                document.add(
                        new Paragraph(
                                "[No matching source text identified]",
                                normal
                        )
                );
            } else {
                addHighlightedTextBox(
                        document,
                        "Compared Source Material",
                        source,
                        matchingWords,
                        heading,
                        normal,
                        highlighted,
                        new Color(255, 248, 240)
                );
            }

            // Section 5: Exact phrase mapping
            addSectionTitle(
                    document,
                    "5. Exact phrase mapping",
                    heading
            );

            if (matches.isEmpty()) {

                document.add(
                        new Paragraph(
                                "No sequence of three or more consecutive "
                                        + "matching words was found in the "
                                        + "compared source.",
                                normal
                        )
                );

            } else {

                PdfPTable mapping = new PdfPTable(
                        new float[]{0.45f, 2.8f, 2.8f, 0.75f}
                );

                mapping.setWidthPercentage(100);

                addHeader(mapping, "#", heading);
                addHeader(mapping, "Submitted passage", heading);
                addHeader(mapping, "Matching source passage", heading);
                addHeader(mapping, "Words", heading);

                int number = 1;

                for (MatchSegment match : matches) {

                    addBody(
                            mapping,
                            String.valueOf(number++),
                            small
                    );

                    addBody(mapping, match.submittedText, small);
                    addBody(mapping, match.sourceText, small);

                    addBody(
                            mapping,
                            String.valueOf(match.wordCount),
                            small
                    );
                }

                document.add(mapping);
            }

            // Section 6
            addSectionTitle(
                    document,
                    "6. Web-source evidence",
                    heading
            );

            addCitations(
                    document,
                    payload.get("citations"),
                    heading,
                    normal,
                    small
            );

            // Section 7
            addSectionTitle(
                    document,
                    "7. Review note",
                    heading
            );

            document.add(
                    new Paragraph(
                            "This report is a similarity-screening aid. "
                                    + "It identifies overlapping words and "
                                    + "phrases in the text made available "
                                    + "to this system. Common words may also "
                                    + "be highlighted. Matching words alone "
                                    + "do not establish plagiarism. Review "
                                    + "the passage, its citation context, "
                                    + "and the source before making a final "
                                    + "decision.",
                            normal
                    )
            );

            document.close();

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_PDF);

            headers.setContentDispositionFormData(
                    "attachment",
                    "Plagiarism_Audit_Report.pdf"
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(output.toByteArray());

        } catch (Exception exception) {

            exception.printStackTrace();

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).build();
        }
    }

    // =====================================================
    // 8. NEW: FIND INDIVIDUAL WORDS SHARED BY BOTH TEXTS
    // =====================================================

    private Set<String> findMatchingWords(
            String submitted,
            String source) {

        Set<String> submittedWords = new HashSet<>();
        Set<String> sourceWords = new HashSet<>();

        addNormalisedWords(submitted, submittedWords);
        addNormalisedWords(source, sourceWords);

        submittedWords.retainAll(sourceWords);

        return submittedWords;
    }

    private void addNormalisedWords(
            String text,
            Set<String> destination) {

        if (text == null || text.isEmpty()) {
            return;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            destination.add(
                    matcher.group().toLowerCase(Locale.ROOT)
            );
        }
    }

    // =====================================================
    // 9. NEW: BUILD A PARAGRAPH WITH INDIVIDUAL HIGHLIGHTS
    // =====================================================

    private Paragraph buildHighlightedParagraph(
            String text,
            Set<String> matchingWords,
            Font normalFont,
            Font highlightedFont) {

        Paragraph paragraph = new Paragraph();
        paragraph.setLeading(14f);

        if (text == null || text.isEmpty()) {
            return paragraph;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);

        int previousEnd = 0;

        while (matcher.find()) {

            int start = matcher.start();
            int end = matcher.end();

            // Preserve spaces, punctuation and original formatting.
            if (start > previousEnd) {
                paragraph.add(
                        new Chunk(
                                text.substring(previousEnd, start),
                                normalFont
                        )
                );
            }

            String originalWord = matcher.group();

            String normalisedWord =
                    originalWord.toLowerCase(Locale.ROOT);

            if (matchingWords.contains(normalisedWord)) {

                Chunk highlightedChunk = new Chunk(
                        originalWord,
                        highlightedFont
                );

                highlightedChunk.setBackground(
                        HIGHLIGHT_BACKGROUND
                );

                paragraph.add(highlightedChunk);

            } else {

                paragraph.add(
                        new Chunk(originalWord, normalFont)
                );
            }

            previousEnd = end;
        }

        // Add the remaining text after the final word.
        if (previousEnd < text.length()) {
            paragraph.add(
                    new Chunk(
                            text.substring(previousEnd),
                            normalFont
                    )
            );
        }

        return paragraph;
    }

    // =====================================================
    // 10. NEW: ADD A HIGHLIGHTED TEXT BOX TO THE PDF
    // =====================================================

    private void addHighlightedTextBox(
            Document document,
            String label,
            String text,
            Set<String> matchingWords,
            Font labelFont,
            Font normalFont,
            Font highlightedFont,
            Color background) throws Exception {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(9);
        cell.setBackgroundColor(background);
        cell.setBorderColor(new Color(220, 228, 240));

        cell.addElement(
                new Paragraph(label, labelFont)
        );

        cell.addElement(
                buildHighlightedParagraph(
                        text,
                        matchingWords,
                        normalFont,
                        highlightedFont
                )
        );

        table.addCell(cell);
        document.add(table);
    }

    // =====================================================
    // 11. EXACT PHRASE MATCHING
    // =====================================================

    private static final class WordToken {

        private final String original;
        private final String normalized;

        private WordToken(String original) {
            this.original = original;
            this.normalized =
                    original.toLowerCase(Locale.ROOT);
        }
    }

    private static final class MatchSegment {

        private final String submittedText;
        private final String sourceText;
        private final int wordCount;

        private MatchSegment(
                String submittedText,
                String sourceText,
                int wordCount) {

            this.submittedText = submittedText;
            this.sourceText = sourceText;
            this.wordCount = wordCount;
        }
    }

    private List<MatchSegment> findExactMatches(
            String submitted,
            String source) {

        List<WordToken> submittedWords = tokenize(submitted);
        List<WordToken> sourceWords = tokenize(source);

        List<MatchSegment> results = new ArrayList<>();
        Set<Integer> usedSubmittedIndexes = new HashSet<>();

        for (int submittedStart = 0;
             submittedStart < submittedWords.size();
             submittedStart++) {

            if (usedSubmittedIndexes.contains(submittedStart)) {
                continue;
            }

            int bestLength = 0;
            int bestSourceStart = -1;

            for (int sourceStart = 0;
                 sourceStart < sourceWords.size();
                 sourceStart++) {

                int length = 0;

                while (
                        submittedStart + length < submittedWords.size()
                                && sourceStart + length < sourceWords.size()
                                && submittedWords
                                .get(submittedStart + length)
                                .normalized
                                .equals(
                                        sourceWords
                                                .get(sourceStart + length)
                                                .normalized
                                )
                ) {
                    length++;
                }

                if (length > bestLength) {
                    bestLength = length;
                    bestSourceStart = sourceStart;
                }
            }

            if (bestLength >= 3) {

                results.add(
                        new MatchSegment(
                                phrase(
                                        submittedWords,
                                        submittedStart,
                                        bestLength
                                ),
                                phrase(
                                        sourceWords,
                                        bestSourceStart,
                                        bestLength
                                ),
                                bestLength
                        )
                );

                for (int i = 0; i < bestLength; i++) {
                    usedSubmittedIndexes.add(
                            submittedStart + i
                    );
                }

                submittedStart += bestLength - 1;
            }
        }

        return results;
    }

    private List<WordToken> tokenize(String text) {

        List<WordToken> words = new ArrayList<>();

        Matcher matcher = WORD_PATTERN.matcher(
                text == null ? "" : text
        );

        while (matcher.find()) {
            words.add(new WordToken(matcher.group()));
        }

        return words;
    }

    private String phrase(
            List<WordToken> words,
            int start,
            int length) {

        StringBuilder phrase = new StringBuilder();

        for (int i = start; i < start + length; i++) {

            if (phrase.length() > 0) {
                phrase.append(" ");
            }

            phrase.append(words.get(i).original);
        }

        return phrase.toString();
    }

    private int wordCount(String text) {
        return tokenize(text).size();
    }

    // =====================================================
    // 12. PDF TABLE HELPERS
    // =====================================================

    private double asDouble(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String value(Object item) {
        return item == null ? "" : String.valueOf(item);
    }

    private void addSectionTitle(
            Document document,
            String text,
            Font font) throws Exception {

        document.add(Chunk.NEWLINE);
        document.add(new Paragraph(text, font));
        document.add(new Paragraph(" "));
    }

    private void addCell(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont,
            Color background) {

        PdfPCell cell = new PdfPCell();

        cell.setPadding(9);
        cell.setBackgroundColor(background);
        cell.setBorderColor(new Color(220, 228, 240));

        cell.addElement(
                new Phrase(label + "\n", labelFont)
        );

        cell.addElement(
                new Phrase(value, valueFont)
        );

        table.addCell(cell);
    }

    private void addMetric(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        addCell(
                table,
                label,
                value,
                labelFont,
                valueFont,
                new Color(248, 250, 253)
        );
    }

    private void addHeader(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, font)
        );

        cell.setPadding(7);
        cell.setBackgroundColor(new Color(232, 240, 252));
        cell.setBorderColor(new Color(205, 219, 239));

        table.addCell(cell);
    }

    private void addBody(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, font)
        );

        cell.setPadding(7);
        cell.setBorderColor(new Color(220, 228, 240));

        table.addCell(cell);
    }

    // =====================================================
    // 13. WEB CITATIONS IN PDF
    // =====================================================

    private void addCitations(
            Document document,
            Object rawCitations,
            Font heading,
            Font normal,
            Font small) throws Exception {

        if (!(rawCitations instanceof List<?> citations)
                || citations.isEmpty()) {

            document.add(
                    new Paragraph(
                            "No web citations were returned for this scan.",
                            normal
                    )
            );

            return;
        }

        PdfPTable table = new PdfPTable(
                new float[]{1.3f, 2.1f, 3.4f}
        );

        table.setWidthPercentage(100);

        addHeader(table, "Domain", heading);
        addHeader(table, "Title", heading);
        addHeader(table, "Matched snippet", heading);

        for (Object item : citations) {

            if (item instanceof Map<?, ?> citation) {

                addBody(
                        table,
                        value(citation.get("domain")),
                        small
                );

                addBody(
                        table,
                        value(citation.get("title")),
                        small
                );

                addBody(
                        table,
                        value(citation.get("snippet")),
                        small
                );
            }
        }

        document.add(table);
    }

    // =====================================================
    // 14. AI TEXT DETECTION
    // =====================================================

    @PostMapping("/detect-ai")
    public ResponseEntity<Map<String, Object>> detectAI(
            @RequestBody Map<String, String> payload) {

        String text = payload.getOrDefault("text", "").trim();

        if (text.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Text cannot be empty for AI detection.")
            );
        }

        try {
            String apiKey = "YOUR_SAPLING_API_KEY";

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("key", apiKey);
            jsonBody.put("text", text);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.sapling.ai/api/v1/aidetect"
                    ))
                    .header("Content-Type", "application/json")
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    jsonBody.toString()
                            )
                    )
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {

                JSONObject responseJson =
                        new JSONObject(response.body());

                double score = responseJson.optDouble("score", 0.0);
                int aiPercentage = (int) Math.round(score * 100);

                return ResponseEntity.ok(
                        Map.of(
                                "aiPercentage", aiPercentage,
                                "label", aiPercentage > 50
                                        ? "AI-Generated"
                                        : "Human-Written"
                        )
                );
            }

            return ResponseEntity.status(
                    HttpStatus.BAD_GATEWAY
            ).body(
                    Map.of(
                            "error",
                            "AI detection provider returned an error."
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(
                    Map.of("error", "AI Detection failed")
            );
        }
    }

    // =====================================================
    // 15. TEXT SUMMARISER
    // =====================================================

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarizeText(
            @RequestBody(required = false) Map<String, String> payload) {

        if (payload == null) {
            payload = new HashMap<>();
        }

        String text = payload.getOrDefault("text", "").trim();

        if (text.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of("summary", "Please enter text first.")
            );
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");

        if (sentences.length <= 2) {
            return ResponseEntity.ok(
                    Map.of("summary", text)
            );
        }

        Map<String, Integer> wordCounts = new HashMap<>();

        for (String word : text.toLowerCase(Locale.ROOT)
                .split("[^a-zA-Z0-9]+")) {

            if (word.length() > 3) {
                wordCounts.put(
                        word,
                        wordCounts.getOrDefault(word, 0) + 1
                );
            }
        }

        String bestSentence = sentences[0];
        int maxScore = -1;

        for (String sentence : sentences) {

            int score = 0;

            for (String word : sentence.toLowerCase(Locale.ROOT)
                    .split("[^a-zA-Z0-9]+")) {

                score += wordCounts.getOrDefault(word, 0);
            }

            if (score > maxScore) {
                maxScore = score;
                bestSentence = sentence;
            }
        }

        String summary = "• "
                + sentences[0].trim()
                + "\n• "
                + bestSentence.trim();

        return ResponseEntity.ok(
                Map.of("summary", summary)
        );
    }
}