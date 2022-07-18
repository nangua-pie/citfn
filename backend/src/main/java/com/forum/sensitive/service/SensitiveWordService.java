package com.forum.sensitive.service;

import java.util.List;

/**
 * Masking of disallowed words in user submitted text.
 */
public interface SensitiveWordService {

    /**
     * Reloads the rule set from the database.
     *
     * <p>Called on a timer and after any administrative change so an edit takes
     * effect immediately rather than at the next refresh.
     */
    void refresh();

    /**
     * Replaces every enabled sensitive word with its configured mask.
     *
     * @return the masked text; the input is returned unchanged when filtering is
     *         disabled or no rule matches
     */
    String mask(String text);

    /** @return true when the text contains at least one enabled sensitive word. */
    boolean containsSensitiveWord(String text);

    /** Admin listing of configured words. */
    List<String> listAllWords();
}
