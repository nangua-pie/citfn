package com.forum.common.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Writes a {@code Long} as a JSON number.
 *
 * <p>Identifiers are emitted as strings by {@code JacksonConfig} so they survive
 * a JavaScript client, but small counters such as a page total sit far below
 * 2^53 and read more naturally as numbers. This serializer opts a field back
 * out. It is written by hand because the stock
 * {@code NumberSerializers.LongSerializer} gets resolved as a Spring bean and
 * fails to instantiate inside {@code @JsonSerialize}.
 */
public class LongAsNumberSerializer extends StdSerializer<Long> {

    private static final long serialVersionUID = 1L;

    public LongAsNumberSerializer() {
        super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.longValue());
        }
    }
}
