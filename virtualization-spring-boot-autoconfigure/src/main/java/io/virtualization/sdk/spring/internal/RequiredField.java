package io.virtualization.sdk.spring.internal;

import io.virtualization.sdk.core.exception.ConfigurationException;

/** Shared validation used by the typed {@code *ProviderProperties} conversions. */
public final class RequiredField {

    private RequiredField() {}

    public static String require(String providerName, String type, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(
                    "Provider '" + providerName + "' has type '" + type + "' but '" + fieldName + "' is missing.");
        }
        return value;
    }
}
