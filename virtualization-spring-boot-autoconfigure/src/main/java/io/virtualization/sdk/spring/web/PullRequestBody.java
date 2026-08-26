package io.virtualization.sdk.spring.web;

/** {@code POST /api/v1/images/pull} request body. {@code remote} is optional for providers with no remote concept. */
record PullRequestBody(String provider, String remote, String identifier) {}
