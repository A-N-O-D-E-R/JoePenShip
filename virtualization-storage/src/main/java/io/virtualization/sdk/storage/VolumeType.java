package io.virtualization.sdk.storage;

/** Matches the backend's real volume-type values. Only {@link #CUSTOM} volumes are created by this SDK. */
public enum VolumeType {
    CUSTOM,
    CONTAINER,
    VIRTUAL_MACHINE,
    IMAGE
}
