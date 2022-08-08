package com.forum.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CORS behaviour of the API.
 *
 * <p>A rejected origin produces a bare HTTP 403 that looks nothing like a CORS
 * failure in the browser, so the loopback origins are pinned by test.
 */
class CorsConfigTest {

    private static final String ORIGINS =
            "http://localhost:5173,http://127.0.0.1:5173";

    private MockHttpServletResponse call(String origin) throws Exception {
        return call(ORIGINS, origin);
    }

    private MockHttpServletResponse call(String configuredOrigins, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
        };
        new CorsConfig().corsFilter(configuredOrigins).doFilter(request, response, chain);
        return response;
    }

    @Test
    @DisplayName("both loopback spellings pass the preflight")
    void bothLoopbackOriginsAllowed() throws Exception {
        for (String origin : new String[] {"http://localhost:5173", "http://127.0.0.1:5173"}) {
            MockHttpServletResponse response = call(origin);
            assertThat(response.getStatus())
                    .as("preflight for %s", origin)
                    .isNotEqualTo(403);
            assertThat(response.getHeader("Access-Control-Allow-Origin"))
                    .as("allow-origin header for %s", origin)
                    .isEqualTo(origin);
        }
    }

    @Test
    @DisplayName("an unrelated origin is refused")
    void foreignOriginRefused() throws Exception {
        MockHttpServletResponse response = call("http://evil.example.com");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    @DisplayName("a wildcard pattern allows any loopback port but nothing else")
    void wildcardAllowsAnyLoopbackPort() throws Exception {
        String configured = "http://localhost:[*],http://127.0.0.1:[*]";

        for (int port : new int[] {5173, 5174, 8081}) {
            MockHttpServletResponse response =
                    call(configured, "http://127.0.0.1:" + port);
            assertThat(response.getHeader("Access-Control-Allow-Origin"))
                    .as("port %d should be allowed", port)
                    .isEqualTo("http://127.0.0.1:" + port);
        }

        MockHttpServletResponse foreign =
                call(configured, "http://attacker.example.com:5173");
        assertThat(foreign.getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
