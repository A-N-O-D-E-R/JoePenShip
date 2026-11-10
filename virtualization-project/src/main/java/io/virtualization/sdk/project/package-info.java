/**
 * Provider-neutral project/tenancy grouping — {@link io.virtualization.sdk.project.Project},
 * {@link io.virtualization.sdk.project.ProjectManager}. First slice of the roadmap's Projects
 * milestone: a project exists as a first-class, independently manageable resource, but no other
 * resource in this SDK (VPS, Domain, Certificate, IAM's own entities, ...) references a {@link
 * io.virtualization.sdk.project.ProjectId} yet — threading project-scoping through those is a
 * separate, later increment. Pure Java, no framework dependency.
 */
package io.virtualization.sdk.project;
