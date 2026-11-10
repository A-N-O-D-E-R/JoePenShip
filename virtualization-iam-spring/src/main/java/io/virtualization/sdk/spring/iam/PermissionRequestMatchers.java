package io.virtualization.sdk.spring.iam;

import io.virtualization.sdk.iam.Permissions;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * {@code /api/v1/**} endpoint -> required permission — the identical catalog {@code
 * io.virtualization.sdk.iam.Permissions} defines, shared with the CLI's {@code requirePermission}
 * calls so a command and its REST equivalent require the same permission. Order matters: a more
 * specific pattern (e.g. {@code /api/v1/images/search}) must be listed before a more general one
 * it would otherwise be shadowed by (e.g. {@code /api/v1/images/**}) — Spring Security's {@code
 * authorizeHttpRequests} matches in registration order, first match wins.
 */
final class PermissionRequestMatchers {

    private PermissionRequestMatchers() {}

    record Entry(HttpMethod method, String pattern, String permission) {}

    static final List<Entry> ENTRIES = List.of(
            new Entry(HttpMethod.GET, "/api/v1/images", Permissions.IMAGE_LIST),
            new Entry(HttpMethod.GET, "/api/v1/images/search", Permissions.IMAGE_SEARCH),
            new Entry(HttpMethod.GET, "/api/v1/images/download/**", Permissions.IMAGE_DOWNLOAD),
            new Entry(HttpMethod.GET, "/api/v1/images/**", Permissions.IMAGE_GET),
            new Entry(HttpMethod.POST, "/api/v1/images/pull", Permissions.IMAGE_PULL),
            new Entry(HttpMethod.POST, "/api/v1/images/import", Permissions.IMAGE_IMPORT),

            new Entry(HttpMethod.POST, "/api/v1/workloads", Permissions.WORKLOAD_CREATE),

            new Entry(HttpMethod.POST, "/api/v1/vps", Permissions.VPS_CREATE),
            new Entry(HttpMethod.GET, "/api/v1/vps", Permissions.VPS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/vps/*/start", Permissions.VPS_START),
            new Entry(HttpMethod.POST, "/api/v1/vps/*/stop", Permissions.VPS_STOP),
            new Entry(HttpMethod.POST, "/api/v1/vps/*/restart", Permissions.VPS_RESTART),
            new Entry(HttpMethod.POST, "/api/v1/vps/*/shutdown", Permissions.VPS_SHUTDOWN),
            new Entry(HttpMethod.POST, "/api/v1/vps/*/rebuild", Permissions.VPS_REBUILD),
            new Entry(HttpMethod.DELETE, "/api/v1/vps/*", Permissions.VPS_DESTROY),
            new Entry(HttpMethod.GET, "/api/v1/vps/*", Permissions.VPS_GET),

            new Entry(HttpMethod.GET, "/api/v1/domains", Permissions.DOMAIN_LIST),
            new Entry(HttpMethod.GET, "/api/v1/domains/*/records", Permissions.DOMAIN_RECORDS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/domains/*/records", Permissions.DOMAIN_RECORDS_CREATE),
            new Entry(HttpMethod.DELETE, "/api/v1/domains/*/records/*", Permissions.DOMAIN_RECORDS_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/domains/*", Permissions.DOMAIN_GET),

            new Entry(HttpMethod.GET, "/api/v1/certificates", Permissions.CERTIFICATE_LIST),
            new Entry(HttpMethod.POST, "/api/v1/certificates", Permissions.CERTIFICATE_REQUEST),
            new Entry(HttpMethod.POST, "/api/v1/certificates/*/renew", Permissions.CERTIFICATE_RENEW),
            new Entry(HttpMethod.POST, "/api/v1/certificates/*/revoke", Permissions.CERTIFICATE_REVOKE),
            new Entry(HttpMethod.GET, "/api/v1/certificates/*", Permissions.CERTIFICATE_GET),

            new Entry(HttpMethod.GET, "/api/v1/iam/users", Permissions.IAM_USERS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/iam/users", Permissions.IAM_USERS_CREATE),
            new Entry(HttpMethod.POST, "/api/v1/iam/users/*/enable", Permissions.IAM_USERS_ENABLE),
            new Entry(HttpMethod.POST, "/api/v1/iam/users/*/disable", Permissions.IAM_USERS_DISABLE),
            new Entry(HttpMethod.POST, "/api/v1/iam/users/*/password", Permissions.IAM_USERS_SET_PASSWORD),
            new Entry(HttpMethod.PUT, "/api/v1/iam/users/*/roles/*", Permissions.IAM_USERS_ASSIGN_ROLE),
            new Entry(HttpMethod.DELETE, "/api/v1/iam/users/*/roles/*", Permissions.IAM_USERS_REMOVE_ROLE),
            new Entry(HttpMethod.GET, "/api/v1/iam/users/*", Permissions.IAM_USERS_GET),

            new Entry(HttpMethod.GET, "/api/v1/iam/roles", Permissions.IAM_ROLES_LIST),
            new Entry(HttpMethod.POST, "/api/v1/iam/roles", Permissions.IAM_ROLES_CREATE),
            new Entry(HttpMethod.POST, "/api/v1/iam/roles/*/permissions/grant", Permissions.IAM_ROLES_GRANT_PERMISSION),
            new Entry(HttpMethod.POST, "/api/v1/iam/roles/*/permissions/revoke", Permissions.IAM_ROLES_REVOKE_PERMISSION),
            new Entry(HttpMethod.DELETE, "/api/v1/iam/roles/*", Permissions.IAM_ROLES_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/iam/roles/*", Permissions.IAM_ROLES_GET),

            new Entry(HttpMethod.GET, "/api/v1/iam/groups", Permissions.IAM_GROUPS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/iam/groups", Permissions.IAM_GROUPS_CREATE),
            new Entry(HttpMethod.POST, "/api/v1/iam/groups/*/members/*", Permissions.IAM_GROUPS_ADD_MEMBER),
            new Entry(HttpMethod.DELETE, "/api/v1/iam/groups/*/members/*", Permissions.IAM_GROUPS_REMOVE_MEMBER),
            new Entry(HttpMethod.POST, "/api/v1/iam/groups/*/roles/*", Permissions.IAM_GROUPS_ASSIGN_ROLE),
            new Entry(HttpMethod.DELETE, "/api/v1/iam/groups/*/roles/*", Permissions.IAM_GROUPS_REMOVE_ROLE),
            new Entry(HttpMethod.DELETE, "/api/v1/iam/groups/*", Permissions.IAM_GROUPS_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/iam/groups/*", Permissions.IAM_GROUPS_GET),

            new Entry(HttpMethod.POST, "/api/v1/iam/credentials", Permissions.IAM_CREDENTIALS_ISSUE),
            new Entry(HttpMethod.GET, "/api/v1/iam/credentials", Permissions.IAM_CREDENTIALS_LIST),
            new Entry(HttpMethod.DELETE, "/api/v1/iam/credentials/*", Permissions.IAM_CREDENTIALS_REVOKE),

            new Entry(HttpMethod.GET, "/api/v1/projects", Permissions.PROJECT_LIST),
            new Entry(HttpMethod.POST, "/api/v1/projects", Permissions.PROJECT_CREATE),
            new Entry(HttpMethod.DELETE, "/api/v1/projects/*", Permissions.PROJECT_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/projects/*", Permissions.PROJECT_GET),

            new Entry(HttpMethod.GET, "/api/v1/networks", Permissions.NETWORK_LIST),
            new Entry(HttpMethod.POST, "/api/v1/networks", Permissions.NETWORK_CREATE),
            new Entry(HttpMethod.GET, "/api/v1/networks/*/subnets/*/ips", Permissions.NETWORK_IPS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/networks/*/subnets/*/ips", Permissions.NETWORK_IPS_ALLOCATE),
            new Entry(HttpMethod.DELETE, "/api/v1/networks/*/subnets/*/ips/*", Permissions.NETWORK_IPS_RELEASE),
            new Entry(HttpMethod.GET, "/api/v1/networks/*/subnets", Permissions.NETWORK_SUBNETS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/networks/*/subnets", Permissions.NETWORK_SUBNETS_CREATE),
            new Entry(HttpMethod.DELETE, "/api/v1/networks/*/subnets/*", Permissions.NETWORK_SUBNETS_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/networks/*/subnets/*", Permissions.NETWORK_SUBNETS_GET),
            new Entry(HttpMethod.GET, "/api/v1/networks/*/firewall-rules", Permissions.NETWORK_FIREWALL_RULES_LIST),
            new Entry(HttpMethod.POST, "/api/v1/networks/*/firewall-rules", Permissions.NETWORK_FIREWALL_RULES_CREATE),
            new Entry(HttpMethod.DELETE, "/api/v1/networks/*/firewall-rules/*", Permissions.NETWORK_FIREWALL_RULES_DELETE),
            new Entry(HttpMethod.DELETE, "/api/v1/networks/*", Permissions.NETWORK_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/networks/*", Permissions.NETWORK_GET),

            new Entry(HttpMethod.GET, "/api/v1/storage/pools", Permissions.STORAGE_POOLS_LIST),
            new Entry(HttpMethod.GET, "/api/v1/storage/pools/*", Permissions.STORAGE_POOLS_GET),
            new Entry(HttpMethod.POST, "/api/v1/storage/pools/*/volumes/*/snapshots/*/restore", Permissions.STORAGE_VOLUMES_SNAPSHOTS_RESTORE),
            new Entry(HttpMethod.DELETE, "/api/v1/storage/pools/*/volumes/*/snapshots/*", Permissions.STORAGE_VOLUMES_SNAPSHOTS_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/storage/pools/*/volumes/*/snapshots", Permissions.STORAGE_VOLUMES_SNAPSHOTS_LIST),
            new Entry(HttpMethod.POST, "/api/v1/storage/pools/*/volumes/*/snapshots", Permissions.STORAGE_VOLUMES_SNAPSHOTS_CREATE),
            new Entry(HttpMethod.POST, "/api/v1/storage/pools/*/volumes/*/resize", Permissions.STORAGE_VOLUMES_RESIZE),
            new Entry(HttpMethod.GET, "/api/v1/storage/pools/*/volumes", Permissions.STORAGE_VOLUMES_LIST),
            new Entry(HttpMethod.POST, "/api/v1/storage/pools/*/volumes", Permissions.STORAGE_VOLUMES_CREATE),
            new Entry(HttpMethod.DELETE, "/api/v1/storage/pools/*/volumes/*", Permissions.STORAGE_VOLUMES_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/storage/pools/*/volumes/*", Permissions.STORAGE_VOLUMES_GET),

            new Entry(HttpMethod.GET, "/api/v1/load-balancers", Permissions.LOADBALANCER_LIST),
            new Entry(HttpMethod.POST, "/api/v1/load-balancers", Permissions.LOADBALANCER_CREATE),
            new Entry(HttpMethod.GET, "/api/v1/load-balancers/*/routes", Permissions.LOADBALANCER_ROUTES_LIST),
            new Entry(HttpMethod.POST, "/api/v1/load-balancers/*/routes", Permissions.LOADBALANCER_ROUTES_CREATE),
            new Entry(HttpMethod.PUT, "/api/v1/load-balancers/*/routes/*", Permissions.LOADBALANCER_ROUTES_UPDATE),
            new Entry(HttpMethod.DELETE, "/api/v1/load-balancers/*/routes/*", Permissions.LOADBALANCER_ROUTES_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/load-balancers/*/routes/*", Permissions.LOADBALANCER_ROUTES_GET),
            new Entry(HttpMethod.DELETE, "/api/v1/load-balancers/*", Permissions.LOADBALANCER_DELETE),
            new Entry(HttpMethod.GET, "/api/v1/load-balancers/*", Permissions.LOADBALANCER_GET));
}
