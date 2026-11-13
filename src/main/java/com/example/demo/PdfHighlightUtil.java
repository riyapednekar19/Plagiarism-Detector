
package com.example.demo;

import com.lowagie.text.Chunk;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.WebColors;

import java.awt.Color;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfHighlightUtil {

    private static final Color HIGHLIGHT_BG =
            WebColors.getRGBColor("#FFF176");

    private static final Color HIGHLIGHT_TEXT =
            WebColors.getRGBColor("#9B1C1C");

    private static final Font NORMAL_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA, 8.5f, Color.BLACK);

    private static final Font HIGHLIGHT_FONT =
            FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    8.5f,
                    HIGHLIGHT_TEXT);

    private static final Pattern WORD_PATTERN =
            Pattern.compile("[\\p{L}\\p{N}]+");

    public static Paragraph buildHighlightedParagraph(
            String sourceText, String suspectText) {

        Paragraph paragraph = new Paragraph();
        paragraph.setLeading(14f);

        if (suspectText == null || suspectText.isEmpty()) {
            return paragraph;
        }

        Set<String> sourceWords = extractWords(sourceText);

        Matcher matcher = WORD_PATTERN.matcher(suspectText);
        int lastEnd = 0;

        while (matcher.find()) {

            if (matcher.start() > lastEnd) {
                paragraph.add(new Chunk(
                        suspectText.substring(lastEnd, matcher.start()),
                        NORMAL_FONT));
            }

            String word = matcher.group();
            String normalised = word.toLowerCase(Locale.ROOT);

            if (sourceWords.contains(normalised)) {
                Chunk matchChunk = new Chunk(word, HIGHLIGHT_FONT);
                matchChunk.setBackground(HIGHLIGHT_BG);
                paragraph.add(matchChunk);
            } else {
                paragraph.add(new Chunk(word, NORMAL_FONT));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < suspectText.length()) {
            paragraph.add(new Chunk(
                    suspectText.substring(lastEnd),
                    NORMAL_FONT));
        }

        return paragraph;
    }

    private static Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return words;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            words.add(matcher.group().toLowerCase(Locale.ROOT));
        }

        return words;
    }
}
