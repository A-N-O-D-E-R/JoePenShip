package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** The only {@link SubnetManager} implementation. */
public final class DefaultSubnetManager implements SubnetManager {

    private final SubnetRepository repository;
    private final NetworkRepository networkRepository;

    public DefaultSubnetManager(SubnetRepository repository, NetworkRepository networkRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.networkRepository = Objects.requireNonNull(networkRepository, "networkRepository must not be null");
    }

    @Override
    public Subnet create(NetworkId networkId, String cidr, String gateway) {
        Objects.requireNonNull(networkId, "networkId must not be null");
        Objects.requireNonNull(cidr, "cidr must not be null");
        if (networkRepository.findById(networkId).isEmpty()) {
            throw new ResourceNotFoundException("No network with id '" + networkId.value() + "'");
        }
        Subnet subnet = new Subnet(SubnetId.generate(), networkId, cidr, gateway, Instant.now());
        rejectOverlap(subnet);
        repository.save(subnet);
        return subnet;
    }

    /**
     * Overlap is checked only against other subnets in the **same** network — different networks
     * are isolated L2 domains and may legitimately reuse the same CIDR range.
     */
    private void rejectOverlap(Subnet candidate) {
        if (!candidate.cidrBlock().isIpv4()) {
            return;
        }
        for (Subnet existing : repository.findByNetwork(candidate.networkId())) {
            if (existing.cidrBlock().isIpv4() && candidate.cidrBlock().overlaps(existing.cidrBlock())) {
                throw new IllegalArgumentException(
                        "CIDR '" + candidate.cidr() + "' overlaps existing subnet '" + existing.cidr() + "' in network '" + candidate.networkId().value() + "'");
            }
        }
    }

    @Override
    public Subnet get(SubnetId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No subnet with id '" + id.value() + "'"));
    }

    @Override
    public List<Subnet> list(NetworkId networkId) {
        Objects.requireNonNull(networkId, "networkId must not be null");
        return repository.findByNetwork(networkId);
    }

    @Override
    public void delete(SubnetId id) {
        get(id);
        repository.delete(id);
    }
}
