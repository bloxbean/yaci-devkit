package com.bloxbean.cardano.yacicli.util;

import com.samskivert.mustache.Mustache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Map;

@Component
public class AdvancedTemplateEngine {
    @Autowired
    private Mustache.Compiler mustacheCompiler;

    public void replaceValues(Path templateFile, Path file, Map<String, String> valuesMap) throws Exception {
        if (!templateFile.toFile().exists()) {
            ConsoleWriter.error("Template file not found : " + templateFile.toString());
            return;
        }

        try (FileReader fileReader = new FileReader(templateFile.toFile()); FileWriter fileWriter = new FileWriter(file.toFile())) {
            var template = mustacheCompiler.compile(fileReader);
            template.execute(valuesMap, fileWriter);
        }
    }
}
