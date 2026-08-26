package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.certificate.ChallengeType;

import java.util.List;

/** {@code POST /api/v1/certificates} request body. {@code challenge} defaults to {@code DNS_01} if omitted. */
record CreateCertificateRequestBody(List<String> domains, String issuer, ChallengeType challenge) {}
