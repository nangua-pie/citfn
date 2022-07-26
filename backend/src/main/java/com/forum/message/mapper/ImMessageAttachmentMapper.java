package com.forum.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.message.entity.ImMessageAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Message attachment access.
 */
@Mapper
public interface ImMessageAttachmentMapper extends BaseMapper<ImMessageAttachment> {

    @Select("SELECT * FROM im_message_attachment WHERE message_id = #{messageId} ORDER BY sort_order, id")
    List<ImMessageAttachment> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * Bulk attachment lookup.
     *
     * <p>The {@code <script>} wrapper is required: without it the
     * {@code <foreach>} is not parsed and MyBatis fails at runtime with
     * "Parameter 'id' not found".
     */
    @Select("<script>"
            + "SELECT * FROM im_message_attachment WHERE message_id IN "
            + "<foreach collection='messageIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY message_id, sort_order, id"
            + "</script>")
    List<ImMessageAttachment> selectByMessageIds(@Param("messageIds") List<Long> messageIds);
}
