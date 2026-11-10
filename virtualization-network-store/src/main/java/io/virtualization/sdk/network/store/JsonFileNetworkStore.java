package io.virtualization.sdk.network.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.virtualization.sdk.network.FirewallRule;
import io.virtualization.sdk.network.FirewallRuleId;
import io.virtualization.sdk.network.FirewallRuleRepository;
import io.virtualization.sdk.network.IpAllocation;
import io.virtualization.sdk.network.IpAllocationId;
import io.virtualization.sdk.network.IpAllocationRepository;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkId;
import io.virtualization.sdk.network.NetworkRepository;
import io.virtualization.sdk.network.Subnet;
import io.virtualization.sdk.network.SubnetId;
import io.virtualization.sdk.network.SubnetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A durable {@link NetworkRepository}/{@link SubnetRepository}/{@link IpAllocationRepository}/
 * {@link FirewallRuleRepository} backed by a single JSON file — the default store, not an
 * in-memory one (unlike {@code virtualization-project}): losing IP allocation state on restart
 * risks handing out an address already live on a running VPS, a real conflict rather than merely
 * inconvenient state loss.
 *
 * <p>One file, not four: these aggregates cross-reference each other by id ({@code Subnet.networkId},
 * {@code IpAllocation.subnetId}, {@code FirewallRule.networkId}), so a single file keeps a
 * network deletion's read-modify-write atomic instead of racing across separately-locked files.
 * Same whole-file-read-modify-write pattern as {@code JsonFileIamStore} — every view method below
 * runs its entire read-modify-write under this instance's own monitor.
 *
 * <p>ponytail: in-process lock only, no OS-level file locking — same accepted tradeoff as every
 * other {@code JsonFile*} repository in this codebase.
 */
public final class JsonFileNetworkStore {

    private static final Logger log = LoggerFactory.getLogger(JsonFileNetworkStore.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Set<PosixFilePermission> OWNER_READ_WRITE = PosixFilePermissions.fromString("rw-------");

    private final Path file;
    private final NetworkRepository networks = new NetworkRepositoryView();
    private final SubnetRepository subnets = new SubnetRepositoryView();
    private final IpAllocationRepository ipAllocations = new IpAllocationRepositoryView();
    private final FirewallRuleRepository firewallRules = new FirewallRuleRepositoryView();

    public JsonFileNetworkStore(Path file) {
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    public NetworkRepository networks() {
        return networks;
    }

    public SubnetRepository subnets() {
        return subnets;
    }

    public IpAllocationRepository ipAllocations() {
        return ipAllocations;
    }

    public FirewallRuleRepository firewallRules() {
        return firewallRules;
    }

    private NetworkFile readAll() {
        if (!Files.exists(file)) {
            return NetworkFile.EMPTY;
        }
        try {
            return MAPPER.readValue(file.toFile(), NetworkFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read network state file '" + file + "'", e);
        }
    }

    private void writeAll(NetworkFile state) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), state);
            setOwnerOnlyPermissionsBestEffort();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write network state file '" + file + "'", e);
        }
    }

    private void setOwnerOnlyPermissionsBestEffort() {
        try {
            Files.setPosixFilePermissions(file, OWNER_READ_WRITE);
        } catch (UnsupportedOperationException e) {
            // non-POSIX filesystem (e.g. Windows) — ponytail: no ACL-based equivalent implemented.
        } catch (IOException e) {
            log.warn("Failed to restrict permissions on network state file '{}': {}", file, e.getMessage());
        }
    }

    private static <ID, T> Map<ID, T> indexById(List<T> items, Function<T, ID> idFn) {
        Map<ID, T> map = new LinkedHashMap<>();
        for (T item : items) {
            map.put(idFn.apply(item), item);
        }
        return map;
    }

    private final class NetworkRepositoryView implements NetworkRepository {

        @Override
        public void save(Network network) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                Map<NetworkId, Network> byId = indexById(state.networks(), Network::id);
                byId.put(network.id(), network);
                writeAll(new NetworkFile(List.copyOf(byId.values()), state.subnets(), state.ipAllocations(), state.firewallRules()));
            }
        }

        @Override
        public Optional<Network> findById(NetworkId id) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().networks().stream().filter(n -> n.id().equals(id)).findFirst();
            }
        }

        @Override
        public Optional<Network> findByName(String name) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().networks().stream().filter(n -> n.name().equals(name)).findFirst();
            }
        }

        @Override
        public List<Network> findAll() {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().networks();
            }
        }

        @Override
        public void delete(NetworkId id) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                List<Network> remaining = state.networks().stream().filter(n -> !n.id().equals(id)).toList();
                writeAll(new NetworkFile(remaining, state.subnets(), state.ipAllocations(), state.firewallRules()));
            }
        }
    }

    private final class SubnetRepositoryView implements SubnetRepository {

        @Override
        public void save(Subnet subnet) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                Map<SubnetId, Subnet> byId = indexById(state.subnets(), Subnet::id);
                byId.put(subnet.id(), subnet);
                writeAll(new NetworkFile(state.networks(), List.copyOf(byId.values()), state.ipAllocations(), state.firewallRules()));
            }
        }

        @Override
        public Optional<Subnet> findById(SubnetId id) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().subnets().stream().filter(s -> s.id().equals(id)).findFirst();
            }
        }

        @Override
        public List<Subnet> findByNetwork(NetworkId networkId) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().subnets().stream().filter(s -> s.networkId().equals(networkId)).toList();
            }
        }

        @Override
        public List<Subnet> findAll() {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().subnets();
            }
        }

        @Override
        public void delete(SubnetId id) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                List<Subnet> remaining = state.subnets().stream().filter(s -> !s.id().equals(id)).toList();
                writeAll(new NetworkFile(state.networks(), remaining, state.ipAllocations(), state.firewallRules()));
            }
        }
    }

    private final class IpAllocationRepositoryView implements IpAllocationRepository {

        @Override
        public void save(IpAllocation allocation) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                Map<IpAllocationId, IpAllocation> byId = indexById(state.ipAllocations(), IpAllocation::id);
                byId.put(allocation.id(), allocation);
                writeAll(new NetworkFile(state.networks(), state.subnets(), List.copyOf(byId.values()), state.firewallRules()));
            }
        }

        @Override
        public Optional<IpAllocation> findById(IpAllocationId id) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().ipAllocations().stream().filter(a -> a.id().equals(id)).findFirst();
            }
        }

        @Override
        public List<IpAllocation> findBySubnet(SubnetId subnetId) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().ipAllocations().stream().filter(a -> a.subnetId().equals(subnetId)).toList();
            }
        }

        @Override
        public Optional<IpAllocation> findByAddress(SubnetId subnetId, String address) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().ipAllocations().stream()
                        .filter(a -> a.subnetId().equals(subnetId) && a.address().equals(address))
                        .findFirst();
            }
        }

        @Override
        public List<IpAllocation> findAll() {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().ipAllocations();
            }
        }

        @Override
        public void delete(IpAllocationId id) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                List<IpAllocation> remaining = state.ipAllocations().stream().filter(a -> !a.id().equals(id)).toList();
                writeAll(new NetworkFile(state.networks(), state.subnets(), remaining, state.firewallRules()));
            }
        }
    }

    private final class FirewallRuleRepositoryView implements FirewallRuleRepository {

        @Override
        public void save(FirewallRule rule) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                Map<FirewallRuleId, FirewallRule> byId = indexById(state.firewallRules(), FirewallRule::id);
                byId.put(rule.id(), rule);
                writeAll(new NetworkFile(state.networks(), state.subnets(), state.ipAllocations(), List.copyOf(byId.values())));
            }
        }

        @Override
        public Optional<FirewallRule> findById(FirewallRuleId id) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().firewallRules().stream().filter(r -> r.id().equals(id)).findFirst();
            }
        }

        @Override
        public List<FirewallRule> findByNetwork(NetworkId networkId) {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().firewallRules().stream().filter(r -> r.networkId().equals(networkId)).toList();
            }
        }

        @Override
        public List<FirewallRule> findAll() {
            synchronized (JsonFileNetworkStore.this) {
                return readAll().firewallRules();
            }
        }

        @Override
        public void delete(FirewallRuleId id) {
            synchronized (JsonFileNetworkStore.this) {
                NetworkFile state = readAll();
                List<FirewallRule> remaining = state.firewallRules().stream().filter(r -> !r.id().equals(id)).toList();
                writeAll(new NetworkFile(state.networks(), state.subnets(), state.ipAllocations(), remaining));
            }
        }
    }
}
