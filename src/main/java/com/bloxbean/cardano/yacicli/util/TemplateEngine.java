package com.bloxbean.cardano.yacicli.util;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Component
public class TemplateEngine {

    public void replaceValues(Path file, Map<String, String> valuesMap) throws Exception {

        // Initialize StringSubstitutor instance with value map
        StringSubstitutor stringSubstitutor = new StringSubstitutor(valuesMap);

        String content = Files.readString(file);

        // replace value map to template string
        String result = stringSubstitutor.replace(content);
        Files.writeString(file, result, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
