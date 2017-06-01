package com.cerner.jwala.persistence.jpa.domain;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Created by JC043760 on 6/1/2017.
 */
public class JpaMediaDeserializer extends StdDeserializer<JpaMedia> {

    // This constructor is required because we extended StdDeserializer
    public JpaMediaDeserializer() {
        this(null);
    }

    protected JpaMediaDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public JpaMedia deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        final JpaMedia jpaMedia = new JpaMedia();
        jpaMedia.setId(Long.valueOf(jp.getText()));
        return jpaMedia;
    }
}
