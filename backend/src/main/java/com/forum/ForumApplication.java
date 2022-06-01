package com.forum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the forum modular monolith.
 *
 * <p>Modules live in sibling packages under {@code com.forum}: auth, user, rbac,
 * category, post, comment, message, notification, report, sensitive, file and common.
 */
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@SpringBootApplication
@MapperScan("com.forum.**.mapper")
public class ForumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForumApplication.class, args);
    }
}
