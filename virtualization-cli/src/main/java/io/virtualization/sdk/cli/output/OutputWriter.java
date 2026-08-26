package io.virtualization.sdk.cli.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.PrintWriter;

/** Renders a {@link CliResult} in the format selected by {@code --output}. */
public interface OutputWriter {

    void write(CliResult result, PrintWriter out);

    static OutputWriter forFormat(OutputFormat format) {
        return switch (format) {
            case TABLE -> new TableOutputWriter();
            case JSON -> new StructuredOutputWriter(
                    new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT));
            case YAML -> new StructuredOutputWriter(new YAMLMapper().findAndRegisterModules());
        };
    }
}
