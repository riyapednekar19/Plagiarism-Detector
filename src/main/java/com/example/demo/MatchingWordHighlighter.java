package com.example.demo;


import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchingWordHighlighter {

    private static final Pattern WORD_PATTERN =
            Pattern.compile("[\\p{L}\\p{N}]+(?:['’][\\p{L}\\p{N}]+)*");

    public static String highlightMatches(
            String text, Set<String> matchingWords) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = WORD_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(escapeHtml(
                    text.substring(lastEnd, matcher.start())));

            String word = matcher.group();
            String normalised = word.toLowerCase(Locale.ROOT);

            if (matchingWords.contains(normalised)) {
                result.append(
                                "<span style=\"background-color:#fff176;\">")
                        .append(escapeHtml(word))
                        .append("</span>");
            } else {
                result.append(escapeHtml(word));
            }

            lastEnd = matcher.end();
        }

        result.append(escapeHtml(text.substring(lastEnd)));
        return result.toString();
    }

    public static Set<String> findMatchingWords(
            String textA, String textB) {

        Set<String> wordsA = extractWords(textA);
        Set<String> wordsB = extractWords(textB);

        wordsA.retainAll(wordsB);
        return wordsA;
    }

    private static Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();

        if (text == null) {
            return words;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            words.add(matcher.group().toLowerCase(Locale.ROOT));
        }

        return words;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
