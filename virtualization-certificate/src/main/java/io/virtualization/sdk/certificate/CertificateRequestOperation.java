package io.virtualization.sdk.certificate;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;

import java.util.Optional;

/** A read-only view of an asynchronous {@link AcmeProvider#request} in progress. */
public interface CertificateRequestOperation extends Operation {

    /** Known immediately — generated before issuance starts, unlike a CA-assigned id. */
    CertificateId certificateId();

    /** The resulting {@link Certificate}, populated once this reaches {@link OperationStatus#SUCCEEDED}. */
    Optional<Certificate> certificate();
}
