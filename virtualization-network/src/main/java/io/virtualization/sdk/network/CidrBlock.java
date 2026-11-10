package io.virtualization.sdk.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * IPv4 CIDR block value type — parses {@code "a.b.c.d/n"}, exposes network/broadcast addresses
 * and a lazy iterator over usable host addresses (excludes network+broadcast for prefix lengths
 * under 31; RFC 3021-aware for {@code /31}; a single usable address for {@code /32}). IPv6 CIDRs
 * are recognized ({@link #isIpv4()} returns {@code false}) so a {@link Subnet} can still store and
 * validate one, but every address-math operation throws {@link UnsupportedOperationException} —
 * iterating a {@code /64}-or-larger address space isn't practical, a disclosed limitation.
 */
final class CidrBlock {

    private final boolean ipv4;
    private final int prefixLength;
    private final int networkInt;

    private CidrBlock(boolean ipv4, int prefixLength, int networkInt) {
        this.ipv4 = ipv4;
        this.prefixLength = prefixLength;
        this.networkInt = networkInt;
    }

    static CidrBlock parse(String cidr) {
        Objects.requireNonNull(cidr, "cidr must not be null");
        int slash = cidr.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("invalid CIDR '" + cidr + "': missing '/prefix'");
        }
        String address = cidr.substring(0, slash);
        int prefixLength;
        try {
            prefixLength = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid CIDR '" + cidr + "': non-numeric prefix length");
        }

        InetAddress parsed;
        try {
            parsed = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("invalid CIDR '" + cidr + "': unparseable address");
        }

        if (parsed instanceof Inet4Address) {
            if (prefixLength < 0 || prefixLength > 32) {
                throw new IllegalArgumentException("invalid CIDR '" + cidr + "': IPv4 prefix length must be 0-32");
            }
            int addressInt = toInt(parsed.getAddress());
            int mask = prefixLength == 0 ? 0 : (int) (0xFFFFFFFFL << (32 - prefixLength));
            return new CidrBlock(true, prefixLength, addressInt & mask);
        }
        if (prefixLength < 0 || prefixLength > 128) {
            throw new IllegalArgumentException("invalid CIDR '" + cidr + "': IPv6 prefix length must be 0-128");
        }
        return new CidrBlock(false, prefixLength, 0);
    }

    boolean isIpv4() {
        return ipv4;
    }

    String networkAddress() {
        requireIpv4();
        return toIpString(networkInt);
    }

    String broadcastAddress() {
        requireIpv4();
        return toIpString(networkInt | hostMask());
    }

    boolean contains(String ip) {
        requireIpv4();
        int candidate = toInt(parseIpv4(ip).getAddress());
        int mask = prefixLength == 0 ? 0 : (int) (0xFFFFFFFFL << (32 - prefixLength));
        return (candidate & mask) == networkInt;
    }

    boolean overlaps(CidrBlock other) {
        requireIpv4();
        other.requireIpv4();
        int shorter = Math.min(prefixLength, other.prefixLength);
        int mask = shorter == 0 ? 0 : (int) (0xFFFFFFFFL << (32 - shorter));
        return (networkInt & mask) == (other.networkInt & mask);
    }

    /**
     * Excludes the network and broadcast addresses for prefix lengths under 31; a {@code /31}
     * yields both its addresses (RFC 3021), a {@code /32} yields its single address.
     */
    Iterator<String> usableHostAddresses() {
        requireIpv4();
        int mask = hostMask();
        int first = prefixLength >= 31 ? networkInt : networkInt + 1;
        int last = prefixLength >= 31 ? (networkInt | mask) : (networkInt | mask) - 1;
        return new Iterator<>() {
            int next = first;
            boolean done = false;

            @Override
            public boolean hasNext() {
                return !done;
            }

            @Override
            public String next() {
                if (done) {
                    throw new NoSuchElementException();
                }
                String result = toIpString(next);
                if (next == last) {
                    done = true;
                } else {
                    next++;
                }
                return result;
            }
        };
    }

    private int hostMask() {
        return prefixLength == 32 ? 0 : ~((int) (0xFFFFFFFFL << (32 - prefixLength)));
    }

    private void requireIpv4() {
        if (!ipv4) {
            throw new UnsupportedOperationException("operation not supported for an IPv6 CIDR block");
        }
    }

    private static int toInt(byte[] address) {
        return ((address[0] & 0xFF) << 24) | ((address[1] & 0xFF) << 16) | ((address[2] & 0xFF) << 8) | (address[3] & 0xFF);
    }

    private static InetAddress parseIpv4(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!(address instanceof Inet4Address)) {
                throw new IllegalArgumentException("'" + ip + "' is not an IPv4 address");
            }
            return address;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("invalid IPv4 address '" + ip + "'");
        }
    }

    private static String toIpString(int value) {
        return ((value >>> 24) & 0xFF) + "." + ((value >>> 16) & 0xFF) + "." + ((value >>> 8) & 0xFF) + "." + (value & 0xFF);
    }
}
