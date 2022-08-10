package com.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context load smoke test.
 *
 * <p>Every other test instantiates its subject directly, so nothing else would
 * catch a missing bean, a constructor that cannot be satisfied or a bean that
 * fails during initialisation. The database, Flyway and Redis are mocked so
 * this runs without infrastructure; it verifies wiring, not behaviour.
 *
 * <p>It boots a real embedded Tomcat rather than a mock servlet context,
 * because the WebSocket container bean is only usable inside a genuine
 * servlet container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ForumApplicationContextTest {

    @MockBean
    private DataSource dataSource;

    @MockBean
    private org.flywaydb.core.Flyway flyway;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("the application context starts and every module bean is wired")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBean(com.forum.auth.service.AuthService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.user.service.UserService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.category.service.CategoryService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.post.service.PostService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.comment.service.CommentService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.message.service.MessageService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.message.service.MessagePushService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.notification.service.NotificationService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.report.service.ReportService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.sensitive.service.SensitiveWordService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.file.service.FileService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.rbac.service.RbacService.class)).isNotNull();
        assertThat(applicationContext.getBean(com.forum.log.service.OperationLogService.class)).isNotNull();
    }

    @Test
    @DisplayName("every controller is registered as a bean")
    void controllersAreRegistered() {
        Class<?>[] controllers = {
            com.forum.auth.controller.AuthController.class,
            com.forum.category.controller.CategoryController.class,
            com.forum.comment.controller.CommentController.class,
            com.forum.file.controller.FileController.class,
            com.forum.log.controller.OperationLogController.class,
            com.forum.message.controller.MessageController.class,
            com.forum.notification.controller.NotificationController.class,
            com.forum.post.controller.PostController.class,
            com.forum.rbac.controller.RbacController.class,
            com.forum.report.controller.ReportController.class,
            com.forum.sensitive.controller.SensitiveWordController.class,
            com.forum.user.controller.UserController.class,
        };
        for (Class<?> controller : controllers) {
            // Looked up by type: Spring names @RestController beans from the
            // simple class name, not the fully qualified one.
            assertThat(applicationContext.getBeanNamesForType(controller).length)
                    .as("controller %s should be registered", controller.getSimpleName())
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("security and websocket wiring is present")
    void securityAndWebsocketWiring() {
        assertThat(applicationContext.getBean(
                com.forum.common.security.JwtTokenProvider.class)).isNotNull();
        assertThat(applicationContext.getBean(
                com.forum.common.security.TokenBlacklistService.class)).isNotNull();
        assertThat(applicationContext.getBean(
                com.forum.common.config.SecurityConfig.class)).isNotNull();
        assertThat(applicationContext.getBean(
                com.forum.message.websocket.ImPresenceService.class)).isNotNull();
    }

    @Test
    @DisplayName("a profile is readable by anyone while the rest of the user API stays private")
    void profileIsPublicButTheRestOfTheUserApiIsNot() {
        // The datasource is mocked, so the profile request fails somewhere
        // below the security layer. Reaching that point at all is the point:
        // the filter chain let an anonymous caller through.
        ResponseEntity<String> profile = restTemplate.getForEntity("/api/users/1/profile", String.class);
        assertThat(profile.getBody()).doesNotContain("\"code\":40100");

        // Everything else under /api/users still needs a session.
        ResponseEntity<String> me = restTemplate.getForEntity("/api/users/me", String.class);
        assertThat(me.getBody()).contains("\"code\":40100");
    }
}
