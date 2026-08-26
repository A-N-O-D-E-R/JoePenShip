package io.virtualization.sdk.cli.output;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OutputWriterTest {

    private static final VirtualMachine VM =
            new VirtualMachine("vm-1", "web-1", VirtualMachineState.RUNNING, new ComputeResources(2, 2048));

    private String render(OutputFormat format, CliResult result) {
        StringWriter sink = new StringWriter();
        OutputWriter.forFormat(format).write(result, new PrintWriter(sink));
        return sink.toString();
    }

    @Test
    void tableRendersVmList() {
        String output = render(OutputFormat.TABLE, new CliResult.VmList(List.of(VM)));

        assertThat(output).contains("ID").contains("vm-1").contains("web-1").contains("RUNNING");
    }

    @Test
    void tableRendersEmptyVmListAsMessage() {
        String output = render(OutputFormat.TABLE, new CliResult.VmList(List.of()));

        assertThat(output).containsIgnoringCase("no virtual machines");
    }

    @Test
    void tableRendersProviders() {
        ProviderSummary summary = new ProviderSummary("production", "proxmox", Set.of(Capability.START));

        String output = render(OutputFormat.TABLE, new CliResult.Providers(List.of(summary)));

        assertThat(output).contains("production").contains("proxmox").contains("START");
    }

    @Test
    void tableRendersOperationResult() {
        CliResult.OperationResult result = new CliResult.OperationResult("op-1", OperationStatus.SUCCEEDED, null, null);

        String output = render(OutputFormat.TABLE, result);

        assertThat(output).contains("op-1").contains("SUCCEEDED");
    }

    @Test
    void jsonRendersVmListAsRawArray() {
        String output = render(OutputFormat.JSON, new CliResult.VmList(List.of(VM)));

        assertThat(output.strip()).startsWith("[").endsWith("]");
        assertThat(output).contains("\"id\"").contains("vm-1");
    }

    @Test
    void jsonRendersSingleVmAsObject() {
        String output = render(OutputFormat.JSON, new CliResult.Vm(VM));

        assertThat(output.strip()).startsWith("{").endsWith("}");
        assertThat(output).contains("\"vm-1\"");
    }

    @Test
    void yamlRendersVmList() {
        String output = render(OutputFormat.YAML, new CliResult.VmList(List.of(VM)));

        // doesNotContain("\"id\":") rather than a blanket doesNotContain("{"): an empty map field
        // (e.g. metadata: {}) legitimately renders in flow style even within block-style YAML.
        assertThat(output).contains("id:").contains("vm-1").doesNotContain("\"id\":");
    }
}
