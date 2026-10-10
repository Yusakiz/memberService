package com.example.memberplatform;

import java.nio.charset.StandardCharsets;

import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class JsonTestValue {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestValue() {
    }

    static String read(MvcResult result, String field) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = OBJECT_MAPPER.readTree(content);
        return json.get(field).asString();
    }
}
