package com.forum.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serialises every {@code Long} as a JSON string.
 *
 * <p>Identifiers are 64 bit snowflake values such as 2105735182095093762.
 * JavaScript can only represent integers exactly up to 2^53 - 1, so a bare JSON
 * number is silently rounded by every browser: a client that read an id and
 * sent it back would address the wrong row, or none at all. Emitting ids as
 * strings keeps them exact. Fields that are genuinely numeric rather than
 * identifiers, such as the paging counters, opt back in explicitly.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
