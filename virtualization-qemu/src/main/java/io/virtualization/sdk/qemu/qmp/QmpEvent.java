package io.virtualization.sdk.qemu.qmp;

import com.fasterxml.jackson.databind.JsonNode;

/** An asynchronous QMP event, e.g. {@code SHUTDOWN}, {@code RESET}, {@code STOP}, {@code RESUME}. */
public record QmpEvent(String name, JsonNode data) {}
