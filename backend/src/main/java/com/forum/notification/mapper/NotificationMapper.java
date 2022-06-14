package com.forum.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Notification access.
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(1) FROM forum_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Update("UPDATE forum_notification SET is_read = 1, read_time = #{readTime} "
            + "WHERE user_id = #{userId} AND id = #{notificationId} AND is_read = 0")
    int markRead(@Param("userId") Long userId,
                 @Param("notificationId") Long notificationId,
                 @Param("readTime") LocalDateTime readTime);

    @Update("UPDATE forum_notification SET is_read = 1, read_time = #{readTime} "
            + "WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
}
