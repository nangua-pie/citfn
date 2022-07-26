package com.forum.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.message.entity.ImMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Private message table access.
 *
 * <p>History is always read through {@code beforeId}, an index seek on the
 * primary key. Deep offset paging is deliberately absent: it degrades linearly
 * and can be forced into a denial of service.
 */
@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessage> {

    /**
     * Newest-first page of history strictly older than {@code beforeId}.
     * The returned list is reversed by the service so the client receives
     * ascending order.
     *
     * <p>Each user owns a conversation row for the same pair, and a message is
     * filed under its sender's row, so a reader has to look at both rows or
     * they would only ever see what they sent.
     */
    @Select("SELECT * FROM im_message "
            + "WHERE conversation_id IN (#{conversationId}, #{peerConversationId}) AND is_deleted = 0 "
            + "AND (#{beforeId} IS NULL OR id < #{beforeId}) "
            + "ORDER BY id DESC LIMIT #{size}")
    List<ImMessage> selectHistoryBefore(@Param("conversationId") Long conversationId,
                                        @Param("peerConversationId") Long peerConversationId,
                                        @Param("beforeId") Long beforeId,
                                        @Param("size") Integer size);

    /**
     * Marks every message the reader has not read yet as read.
     *
     * <p>Spans both conversation rows of the pair for the same reason history
     * does: a message is filed under its sender's row.
     */
    @Update("UPDATE im_message SET status = 'READ', read_time = #{readTime} "
            + "WHERE conversation_id IN (#{conversationId}, #{peerConversationId}) "
            + "AND receiver_id = #{readerId} "
            + "AND is_deleted = 0 AND status IN ('SENT', 'DELIVERED')")
    int markConversationRead(@Param("conversationId") Long conversationId,
                             @Param("peerConversationId") Long peerConversationId,
                             @Param("readerId") Long readerId,
                             @Param("readTime") java.time.LocalDateTime readTime);

    /** Marks a single message read, for the receipt on one bubble. */
    @Update("UPDATE im_message SET status = 'READ', read_time = #{readTime} "
            + "WHERE id = #{messageId} AND receiver_id = #{readerId} "
            + "AND is_deleted = 0 AND status IN ('SENT', 'DELIVERED')")
    int markSingleRead(@Param("messageId") Long messageId,
                       @Param("readerId") Long readerId,
                       @Param("readTime") java.time.LocalDateTime readTime);

    @Update("UPDATE im_message SET status = 'RECALLED', recall_time = #{recallTime}, content = '' "
            + "WHERE id = #{messageId} AND is_deleted = 0")
    int markRecalled(@Param("messageId") Long messageId,
                     @Param("recallTime") java.time.LocalDateTime recallTime);

    /**
     * Duplicate-send guard.
     *
     * <p>Returns an identical message the sender already pushed inside the
     * idempotency window, so a retried request reuses it instead of inserting
     * twice.
     */
    @Select("SELECT * FROM im_message WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} "
            + "AND content = #{content} AND message_type = #{messageType} "
            + "AND is_deleted = 0 AND send_time > #{since} ORDER BY id DESC LIMIT 1")
    ImMessage selectRecentDuplicate(@Param("senderId") Long senderId,
                                    @Param("receiverId") Long receiverId,
                                    @Param("content") String content,
                                    @Param("messageType") String messageType,
                                    @Param("since") java.time.LocalDateTime since);

    @Select("SELECT COUNT(1) FROM im_message WHERE receiver_id = #{userId} "
            + "AND is_deleted = 0 AND status IN ('SENT', 'DELIVERED')")
    int countUnreadForUser(@Param("userId") Long userId);
}
