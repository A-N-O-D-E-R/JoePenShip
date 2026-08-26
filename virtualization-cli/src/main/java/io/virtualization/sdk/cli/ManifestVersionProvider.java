package io.virtualization.sdk.cli;

import picocli.CommandLine.IVersionProvider;

/** Reads the CLI's version from the executable JAR's manifest, set by the build at package time. */
final class ManifestVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return new String[] {"virtualization " + (version != null ? version : "development")};
    }
}
