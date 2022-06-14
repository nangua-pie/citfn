package com.forum.sensitive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.sensitive.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Sensitive word access.
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    /**
     * Enabled words ordered longest first.
     *
     * <p>The order matters: replacing a longer phrase before its substrings
     * stops a rule from splitting a word that a broader rule should mask.
     */
    @Select("SELECT * FROM forum_sensitive_word WHERE status = 1 ORDER BY CHAR_LENGTH(word) DESC, id")
    List<SensitiveWord> selectEnabled();

    @Select("SELECT COUNT(1) FROM forum_sensitive_word WHERE word = #{word}")
    int countByWord(String word);
}
