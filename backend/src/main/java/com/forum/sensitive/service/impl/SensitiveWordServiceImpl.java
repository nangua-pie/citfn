package com.forum.sensitive.service.impl;

import com.forum.sensitive.entity.SensitiveWord;
import com.forum.sensitive.mapper.SensitiveWordMapper;
import com.forum.sensitive.service.SensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * In-memory sensitive word filter.
 *
 * <p>The word list changes rarely and is read on every post and comment, so it
 * is cached here and refreshed on a timer instead of hitting MariaDB each time.
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final boolean enabled;

    /** Immutable snapshot; replaced wholesale by {@link #refresh()}. */
    private volatile List<SensitiveWord> cache = Collections.emptyList();

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper,
                                    @Value("${forum.sensitive.enabled:true}") boolean enabled) {
        this.sensitiveWordMapper = sensitiveWordMapper;
        this.enabled = enabled;
    }

    @Override
    @Scheduled(initialDelay = 5_000L, fixedDelay = 300_000L)
    public void refresh() {
        try {
            List<SensitiveWord> words = sensitiveWordMapper.selectEnabled();
            List<SensitiveWord> usable = words.stream()
                    .filter(word -> word.getWord() != null && !word.getWord().isBlank())
                    .collect(Collectors.toList());
            this.cache = Collections.unmodifiableList(usable);
            log.info("sensitive word cache refreshed with {} rule(s)", usable.size());
        } catch (RuntimeException ex) {
            // A refresh failure must not stop content from being accepted;
            // the previous snapshot stays in effect.
            log.warn("sensitive word cache refresh failed: {}", ex.getMessage());
        }
    }

    @Override
    public String mask(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return text;
        }
        List<SensitiveWord> words = this.cache;
        if (words.isEmpty()) {
            return text;
        }
        String masked = text;
        for (SensitiveWord word : words) {
            if (containsIgnoreCase(masked, word.getWord())) {
                masked = replaceIgnoreCase(masked, word.getWord(), replacementOf(word));
            }
        }
        return masked;
    }

    @Override
    public boolean containsSensitiveWord(String text) {
        if (!enabled || text == null || text.isEmpty()) {
            return false;
        }
        for (SensitiveWord word : this.cache) {
            if (containsIgnoreCase(text, word.getWord())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> listAllWords() {
        List<String> words = new ArrayList<>(this.cache.size());
        for (SensitiveWord word : this.cache) {
            words.add(word.getWord());
        }
        return words;
    }

    private String replacementOf(SensitiveWord word) {
        return word.getReplaceWith() == null || word.getReplaceWith().isEmpty() ? "***" : word.getReplaceWith();
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String replaceIgnoreCase(String haystack, String needle, String replacement) {
        // String.replace is literal, so regex metacharacters in a rule are safe.
        return haystack.replaceAll("(?i)" + java.util.regex.Pattern.quote(needle),
                java.util.regex.Matcher.quoteReplacement(replacement));
    }
}
