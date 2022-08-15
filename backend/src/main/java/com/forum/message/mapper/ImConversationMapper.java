package com.forum.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.message.entity.ImConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversation table access.
 *
 * <p>Every read and write is scoped by {@code user_id}, so a caller can never
 * reach another participant's conversation row by guessing an id.
 */
@Mapper
public interface ImConversationMapper extends BaseMapper<ImConversation> {

    @Select("SELECT * FROM im_conversation WHERE user_id = #{userId} AND target_user_id = #{targetUserId} "
            + "AND is_deleted = 0")
    ImConversation selectByParticipants(@Param("userId") Long userId,
                                        @Param("targetUserId") Long targetUserId);

    /** Conversation list ordered by pinned first, then most recent activity. */
    @Select("SELECT * FROM im_conversation WHERE user_id = #{userId} AND is_deleted = 0 "
            + "ORDER BY is_pinned DESC, last_message_time DESC, id DESC")
    List<ImConversation> selectListByUser(@Param("userId") Long userId);

    /** Loads a conversation only if the given user participates in it. */
    @Select("SELECT * FROM im_conversation WHERE id = #{conversationId} AND user_id = #{userId} "
            + "AND is_deleted = 0")
    ImConversation selectOwned(@Param("conversationId") Long conversationId,
                               @Param("userId") Long userId);

    @Update("UPDATE im_conversation SET unread_count = 0 WHERE id = #{conversationId} "
            + "AND user_id = #{userId}")
    int clearUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Update("UPDATE im_conversation SET unread_count = unread_count + 1 "
            + "WHERE id = #{conversationId} AND user_id = #{userId}")
    int incrementUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Update("UPDATE im_conversation SET last_message_id = #{messageId}, "
            + "last_message_preview = #{preview}, last_message_time = #{sendTime} "
            + "WHERE id = #{conversationId} AND user_id = #{userId}")
    int touchLastMessage(@Param("conversationId") Long conversationId,
                         @Param("userId") Long userId,
                         @Param("messageId") Long messageId,
                         @Param("preview") String preview,
                         @Param("sendTime") LocalDateTime sendTime);

    /**
     * Replaces the preview when a recalled message is the newest in the thread.
     *
     * <p>Guarded by {@code last_message_id} so a recall only rewrites the
     * preview it actually still describes: if a newer message arrived after
     * the recalled one, that newer preview is left alone.
     */
    @Update("UPDATE im_conversation SET last_message_preview = #{preview} "
            + "WHERE user_id = #{userId} AND target_user_id = #{targetUserId} "
            + "AND last_message_id = #{messageId} AND is_deleted = 0")
    int replaceLastMessagePreview(@Param("userId") Long userId,
                                  @Param("targetUserId") Long targetUserId,
                                  @Param("messageId") Long messageId,
                                  @Param("preview") String preview);

    @Update("UPDATE im_conversation SET is_deleted = 1 WHERE id = #{conversationId} AND user_id = #{userId}")
    int softDeleteOwned(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(unread_count), 0) FROM im_conversation "
            + "WHERE user_id = #{userId} AND is_deleted = 0")
    int sumUnread(@Param("userId") Long userId);
}
