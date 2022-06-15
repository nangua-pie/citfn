package com.forum.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Profile projection returned to clients.
 *
 * <p>Deliberately free of email, password hash, IP and last-login metadata so
 * it can be returned to any viewer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer level;
    private Integer points;
    private Integer status;
    private LocalDateTime createTime;
}
