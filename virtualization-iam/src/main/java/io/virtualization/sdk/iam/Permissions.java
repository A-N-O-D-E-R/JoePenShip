package io.virtualization.sdk.iam;

import java.util.Set;

/**
 * The fixed catalog of valid permission strings — one CLI command or REST endpoint per constant.
 * The CLI and {@code virtualization-iam-spring} both reference these constants rather than
 * hand-writing the strings, so a typo fails to compile instead of silently never matching. A CLI
 * command and its REST equivalent (where one exists) share the identical permission.
 */
public final class Permissions {

    private Permissions() {}

    public static final String PROVIDER_LIST = "provider:list";

    public static final String VM_LIST = "vm:list";
    public static final String VM_GET = "vm:get";
    public static final String VM_START = "vm:start";
    public static final String VM_STOP = "vm:stop";
    public static final String VM_REBOOT = "vm:reboot";
    public static final String VM_SHUTDOWN = "vm:shutdown";
    public static final String VM_DESTROY = "vm:destroy";

    public static final String IMAGE_LIST = "image:list";
    public static final String IMAGE_SEARCH = "image:search";
    public static final String IMAGE_GET = "image:get";
    public static final String IMAGE_PULL = "image:pull";
    public static final String IMAGE_DOWNLOAD = "image:download";
    public static final String IMAGE_IMPORT = "image:import";

    public static final String WORKLOAD_CREATE = "workload:create";

    public static final String VPS_CREATE = "vps:create";
    public static final String VPS_LIST = "vps:list";
    public static final String VPS_GET = "vps:get";
    public static final String VPS_START = "vps:start";
    public static final String VPS_STOP = "vps:stop";
    public static final String VPS_RESTART = "vps:restart";
    public static final String VPS_SHUTDOWN = "vps:shutdown";
    public static final String VPS_DESTROY = "vps:destroy";
    public static final String VPS_REBUILD = "vps:rebuild";

    public static final String DOMAIN_LIST = "domain:list";
    public static final String DOMAIN_GET = "domain:get";
    public static final String DOMAIN_RECORDS_LIST = "domain:records:list";
    public static final String DOMAIN_RECORDS_CREATE = "domain:records:create";
    public static final String DOMAIN_RECORDS_DELETE = "domain:records:delete";

    public static final String DNS_ZONES_LIST = "dns:zones:list";
    public static final String DNS_RECORDS_LIST = "dns:records:list";
    public static final String DNS_RECORDS_CREATE = "dns:records:create";
    public static final String DNS_RECORDS_DELETE = "dns:records:delete";

    public static final String CERTIFICATE_LIST = "certificate:list";
    public static final String CERTIFICATE_GET = "certificate:get";
    public static final String CERTIFICATE_REQUEST = "certificate:request";
    public static final String CERTIFICATE_RENEW = "certificate:renew";
    public static final String CERTIFICATE_REVOKE = "certificate:revoke";
    public static final String CERTIFICATE_EXPORT = "certificate:export";

    public static final String IAM_USERS_LIST = "iam:users:list";
    public static final String IAM_USERS_CREATE = "iam:users:create";
    public static final String IAM_USERS_GET = "iam:users:get";
    public static final String IAM_USERS_ENABLE = "iam:users:enable";
    public static final String IAM_USERS_DISABLE = "iam:users:disable";
    public static final String IAM_USERS_SET_PASSWORD = "iam:users:set-password";
    public static final String IAM_USERS_ASSIGN_ROLE = "iam:users:assign-role";
    public static final String IAM_USERS_REMOVE_ROLE = "iam:users:remove-role";

    public static final String IAM_ROLES_LIST = "iam:roles:list";
    public static final String IAM_ROLES_CREATE = "iam:roles:create";
    public static final String IAM_ROLES_GET = "iam:roles:get";
    public static final String IAM_ROLES_DELETE = "iam:roles:delete";
    public static final String IAM_ROLES_GRANT_PERMISSION = "iam:roles:grant-permission";
    public static final String IAM_ROLES_REVOKE_PERMISSION = "iam:roles:revoke-permission";

    public static final String IAM_GROUPS_LIST = "iam:groups:list";
    public static final String IAM_GROUPS_CREATE = "iam:groups:create";
    public static final String IAM_GROUPS_GET = "iam:groups:get";
    public static final String IAM_GROUPS_DELETE = "iam:groups:delete";
    public static final String IAM_GROUPS_ADD_MEMBER = "iam:groups:add-member";
    public static final String IAM_GROUPS_REMOVE_MEMBER = "iam:groups:remove-member";
    public static final String IAM_GROUPS_ASSIGN_ROLE = "iam:groups:assign-role";
    public static final String IAM_GROUPS_REMOVE_ROLE = "iam:groups:remove-role";

    public static final String IAM_CREDENTIALS_ISSUE = "iam:credentials:issue";
    public static final String IAM_CREDENTIALS_LIST = "iam:credentials:list";
    public static final String IAM_CREDENTIALS_REVOKE = "iam:credentials:revoke";

    public static final String PROJECT_LIST = "project:list";
    public static final String PROJECT_CREATE = "project:create";
    public static final String PROJECT_GET = "project:get";
    public static final String PROJECT_DELETE = "project:delete";

    public static final String NETWORK_LIST = "network:list";
    public static final String NETWORK_CREATE = "network:create";
    public static final String NETWORK_GET = "network:get";
    public static final String NETWORK_DELETE = "network:delete";

    public static final String NETWORK_SUBNETS_LIST = "network:subnets:list";
    public static final String NETWORK_SUBNETS_CREATE = "network:subnets:create";
    public static final String NETWORK_SUBNETS_GET = "network:subnets:get";
    public static final String NETWORK_SUBNETS_DELETE = "network:subnets:delete";

    public static final String NETWORK_IPS_ALLOCATE = "network:ips:allocate";
    public static final String NETWORK_IPS_RELEASE = "network:ips:release";
    public static final String NETWORK_IPS_LIST = "network:ips:list";

    public static final String NETWORK_FIREWALL_RULES_LIST = "network:firewall-rules:list";
    public static final String NETWORK_FIREWALL_RULES_CREATE = "network:firewall-rules:create";
    public static final String NETWORK_FIREWALL_RULES_DELETE = "network:firewall-rules:delete";

    public static final String STORAGE_POOLS_LIST = "storage:pools:list";
    public static final String STORAGE_POOLS_GET = "storage:pools:get";

    public static final String STORAGE_VOLUMES_LIST = "storage:volumes:list";
    public static final String STORAGE_VOLUMES_GET = "storage:volumes:get";
    public static final String STORAGE_VOLUMES_CREATE = "storage:volumes:create";
    public static final String STORAGE_VOLUMES_DELETE = "storage:volumes:delete";
    public static final String STORAGE_VOLUMES_RESIZE = "storage:volumes:resize";

    public static final String STORAGE_VOLUMES_SNAPSHOTS_LIST = "storage:volumes:snapshots:list";
    public static final String STORAGE_VOLUMES_SNAPSHOTS_CREATE = "storage:volumes:snapshots:create";
    public static final String STORAGE_VOLUMES_SNAPSHOTS_RESTORE = "storage:volumes:snapshots:restore";
    public static final String STORAGE_VOLUMES_SNAPSHOTS_DELETE = "storage:volumes:snapshots:delete";

    public static final String LOADBALANCER_LIST = "loadbalancer:list";
    public static final String LOADBALANCER_CREATE = "loadbalancer:create";
    public static final String LOADBALANCER_GET = "loadbalancer:get";
    public static final String LOADBALANCER_DELETE = "loadbalancer:delete";

    public static final String LOADBALANCER_ROUTES_LIST = "loadbalancer:routes:list";
    public static final String LOADBALANCER_ROUTES_CREATE = "loadbalancer:routes:create";
    public static final String LOADBALANCER_ROUTES_GET = "loadbalancer:routes:get";
    public static final String LOADBALANCER_ROUTES_UPDATE = "loadbalancer:routes:update";
    public static final String LOADBALANCER_ROUTES_DELETE = "loadbalancer:routes:delete";

    /** Every permission this catalog defines — granted in full to the {@link Bootstrap} admin role. */
    public static final Set<String> ALL = Set.of(
            PROVIDER_LIST,
            VM_LIST, VM_GET, VM_START, VM_STOP, VM_REBOOT, VM_SHUTDOWN, VM_DESTROY,
            IMAGE_LIST, IMAGE_SEARCH, IMAGE_GET, IMAGE_PULL, IMAGE_DOWNLOAD, IMAGE_IMPORT,
            WORKLOAD_CREATE,
            VPS_CREATE, VPS_LIST, VPS_GET, VPS_START, VPS_STOP, VPS_RESTART, VPS_SHUTDOWN, VPS_DESTROY, VPS_REBUILD,
            DOMAIN_LIST, DOMAIN_GET, DOMAIN_RECORDS_LIST, DOMAIN_RECORDS_CREATE, DOMAIN_RECORDS_DELETE,
            DNS_ZONES_LIST, DNS_RECORDS_LIST, DNS_RECORDS_CREATE, DNS_RECORDS_DELETE,
            CERTIFICATE_LIST, CERTIFICATE_GET, CERTIFICATE_REQUEST, CERTIFICATE_RENEW, CERTIFICATE_REVOKE, CERTIFICATE_EXPORT,
            IAM_USERS_LIST, IAM_USERS_CREATE, IAM_USERS_GET, IAM_USERS_ENABLE, IAM_USERS_DISABLE, IAM_USERS_SET_PASSWORD,
            IAM_USERS_ASSIGN_ROLE, IAM_USERS_REMOVE_ROLE,
            IAM_ROLES_LIST, IAM_ROLES_CREATE, IAM_ROLES_GET, IAM_ROLES_DELETE, IAM_ROLES_GRANT_PERMISSION, IAM_ROLES_REVOKE_PERMISSION,
            IAM_GROUPS_LIST, IAM_GROUPS_CREATE, IAM_GROUPS_GET, IAM_GROUPS_DELETE, IAM_GROUPS_ADD_MEMBER, IAM_GROUPS_REMOVE_MEMBER,
            IAM_GROUPS_ASSIGN_ROLE, IAM_GROUPS_REMOVE_ROLE,
            IAM_CREDENTIALS_ISSUE, IAM_CREDENTIALS_LIST, IAM_CREDENTIALS_REVOKE,
            PROJECT_LIST, PROJECT_CREATE, PROJECT_GET, PROJECT_DELETE,
            NETWORK_LIST, NETWORK_CREATE, NETWORK_GET, NETWORK_DELETE,
            NETWORK_SUBNETS_LIST, NETWORK_SUBNETS_CREATE, NETWORK_SUBNETS_GET, NETWORK_SUBNETS_DELETE,
            NETWORK_IPS_ALLOCATE, NETWORK_IPS_RELEASE, NETWORK_IPS_LIST,
            NETWORK_FIREWALL_RULES_LIST, NETWORK_FIREWALL_RULES_CREATE, NETWORK_FIREWALL_RULES_DELETE,
            STORAGE_POOLS_LIST, STORAGE_POOLS_GET,
            STORAGE_VOLUMES_LIST, STORAGE_VOLUMES_GET, STORAGE_VOLUMES_CREATE, STORAGE_VOLUMES_DELETE, STORAGE_VOLUMES_RESIZE,
            STORAGE_VOLUMES_SNAPSHOTS_LIST, STORAGE_VOLUMES_SNAPSHOTS_CREATE, STORAGE_VOLUMES_SNAPSHOTS_RESTORE, STORAGE_VOLUMES_SNAPSHOTS_DELETE,
            LOADBALANCER_LIST, LOADBALANCER_CREATE, LOADBALANCER_GET, LOADBALANCER_DELETE,
            LOADBALANCER_ROUTES_LIST, LOADBALANCER_ROUTES_CREATE, LOADBALANCER_ROUTES_GET, LOADBALANCER_ROUTES_UPDATE, LOADBALANCER_ROUTES_DELETE);
}
