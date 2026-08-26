package io.virtualization.sdk.incus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageAlias;
import io.virtualization.sdk.core.image.ImageCapabilities;
import io.virtualization.sdk.core.image.ImageCapability;
import io.virtualization.sdk.core.image.ImageDownload;
import io.virtualization.sdk.core.image.ImageId;
import io.virtualization.sdk.core.image.ImageImportHandle;
import io.virtualization.sdk.core.image.ImageImportOperation;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImagePullHandle;
import io.virtualization.sdk.core.image.ImagePullOperation;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ImageSource;
import io.virtualization.sdk.core.image.InputStreamImageSource;
import io.virtualization.sdk.core.image.LocalFileImageSource;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.client.dto.ImageAliasDto;
import io.virtualization.sdk.incus.client.dto.ImageDto;
import io.virtualization.sdk.incus.internal.CountingInputStream;
import io.virtualization.sdk.incus.internal.ImageOperationWaiter;
import io.virtualization.sdk.incus.internal.IncusImageMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * {@link ImageProvider} backed by the Incus REST API's local image store.
 *
 * <p>Scoped to a single Incus remote — whichever server the underlying {@link IncusApiClient}
 * talks to — identified by {@code remote} (default {@code "local"}). An {@link ImageReference}
 * naming a different remote for list/get/search/download resolves to nothing: reaching another
 * remote means talking to a different server. Register one {@code IncusImageProvider} per remote
 * you need, the same way multiple {@link IncusProvider}s can be registered under different names
 * in a {@link io.virtualization.sdk.core.ProviderRegistry}.
 *
 * <p>{@link #pull} is the exception: it asks the connected Incus server to fetch an image from one
 * of the {@link IncusRemote}s given at construction (Incus's own built-in defaults — {@code
 * images:}, {@code ubuntu:}, {@code ubuntu-daily:} — unless overridden). The Incus server itself
 * speaks simplestreams to reach those; this SDK never does, so browsing/searching those public
 * catalogs ahead of a pull is not supported.
 *
 * <p>Delete is added in a later phase. Instantiate-from-image lives on {@link IncusProvider},
 * which composes an {@code IncusImageProvider} internally to resolve and pull images.
 */
public final class IncusImageProvider implements ImageProvider {

    private static final String DEFAULT_REMOTE = "local";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ImageCapabilities CAPABILITIES = ImageCapabilities.of(
            ImageCapability.LIST, ImageCapability.INSPECT, ImageCapability.SEARCH, ImageCapability.PULL,
            ImageCapability.DOWNLOAD, ImageCapability.UPLOAD, ImageCapability.INSTANTIATE);

    private final IncusApiClient client;
    private final String remote;
    private final Map<String, IncusRemote> remotes;
    private final Duration operationWaitTimeout;

    public IncusImageProvider(IncusClientConfig config) {
        this(config, DEFAULT_REMOTE);
    }

    public IncusImageProvider(IncusClientConfig config, String remote) {
        this(config, remote, IncusRemote.defaults());
    }

    public IncusImageProvider(IncusClientConfig config, String remote, Map<String, IncusRemote> remotes) {
        this(new IncusApiClient(config), remote, remotes, config.operationWaitTimeout());
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    IncusImageProvider(IncusApiClient client, String remote) {
        this(client, remote, IncusRemote.defaults(), Duration.ofSeconds(5));
    }

    IncusImageProvider(IncusApiClient client, String remote, Map<String, IncusRemote> remotes, Duration operationWaitTimeout) {
        this.client = client;
        this.remote = remote;
        this.remotes = remotes;
        this.operationWaitTimeout = operationWaitTimeout;
    }

    @Override
    public String name() {
        return IncusProvider.TYPE.id();
    }

    /** The Incus remote this provider serves images from (e.g. {@code "local"}). */
    public String remote() {
        return remote;
    }

    /** The remotes {@link #pull} can resolve a reference's remote against. */
    public Map<String, IncusRemote> remotes() {
        return remotes;
    }

    @Override
    public ImageCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public List<Image> list() {
        return client.getList("/images?recursion=1", ImageDto.class).stream()
                .map(IncusImageMapper::toImage)
                .toList();
    }

    @Override
    public Optional<Image> get(ImageReference reference) {
        if (reference.remote() != null && !reference.remote().equals(remote)) {
            return Optional.empty();
        }
        try {
            return Optional.of(fetchImage(resolveFingerprint(reference.identifier())));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Image> search(ImageQuery query) {
        if (query.provider().isPresent() && !query.provider().get().equals(name())) {
            return List.of();
        }
        if (query.remote().isPresent() && !query.remote().get().equals(remote)) {
            return List.of();
        }
        return list().stream()
                .filter(image -> query.architecture().map(a -> a.equalsIgnoreCase(image.architecture())).orElse(true))
                .filter(image -> query.distribution().map(d -> d.equalsIgnoreCase(image.distribution())).orElse(true))
                .filter(image -> query.operatingSystem().map(o -> o.equalsIgnoreCase(image.os())).orElse(true))
                .filter(image -> query.version().map(v -> v.equalsIgnoreCase(image.version())).orElse(true))
                .filter(image -> query.type().map(t -> t == image.type()).orElse(true))
                .filter(image -> query.name().map(n -> containsIgnoreCase(image.name(), n)).orElse(true))
                .toList();
    }

    /**
     * Asks the connected Incus server to pull an image from {@code reference}'s remote (resolved
     * against the {@link IncusRemote}s this provider was constructed with) into its local store.
     * The Incus server does the actual fetch — this method only submits the request and tracks
     * the resulting operation.
     */
    @Override
    public ImagePullOperation pull(ImageReference reference) {
        if (reference.remote() == null || reference.remote().equals(remote)) {
            throw new IllegalArgumentException(
                    "pull requires an ImageReference naming a remote other than '" + remote + "'");
        }
        IncusRemote source = remotes.get(reference.remote());
        if (source == null) {
            throw new UnsupportedCapabilityException(
                    "Unknown Incus remote '" + reference.remote() + "' — pass it via the remotes map");
        }

        String jsonBody = pullRequestBody(source, reference.identifier());
        String operationId = client.postForOperationId("/images", jsonBody);
        ImagePullHandle handle = ImagePullHandle.create(operationId != null ? operationId : "pull-" + UUID.randomUUID());
        if (operationId == null) {
            handle.complete();
        } else {
            Thread.ofVirtual().name("incus-image-pull-" + operationId)
                    .start(() -> ImageOperationWaiter.waitPull(client, operationId, handle, operationWaitTimeout));
        }
        return handle.operation();
    }

    /**
     * Exports an image via {@code GET /images/{fingerprint}/export}, streamed directly from the
     * HTTP response — never buffered in full. The fingerprint doubles as the SHA-256 checksum for
     * a unified (single-file) image export, which is how Incus computes it; a split
     * metadata/rootfs export would not match, but this provider does not distinguish the two.
     */
    @Override
    public ImageDownload download(ImageReference reference) {
        if (reference.remote() != null && !reference.remote().equals(remote)) {
            throw new ResourceNotFoundException("No image '" + reference.identifier() + "' on remote '" + reference.remote() + "'");
        }
        String fingerprint = resolveFingerprint(reference.identifier());
        HttpResponse<InputStream> response = client.getStream("/images/" + fingerprint + "/export");
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        return ImageDownload.of(response.body(), contentLength, contentType, fingerprint, "sha256");
    }

    /** Supports {@link LocalFileImageSource} and {@link InputStreamImageSource}; both are streamed, never buffered. */
    @Override
    public ImageImportOperation importImage(ImageSource source) {
        UploadPayload payload = openUploadSource(source);
        ImageImportHandle handle = ImageImportHandle.create("import-" + UUID.randomUUID());
        Thread.ofVirtual().name(handle.operation().id()).start(() -> runImport(payload, handle));
        return handle.operation();
    }

    /** All image aliases known to this remote. Incus-specific — not part of {@link ImageProvider}. */
    public List<ImageAlias> aliases() {
        return client.getList("/images?recursion=1", ImageDto.class).stream()
                .flatMap(dto -> dto.aliases() == null
                        ? Stream.<ImageAlias>empty()
                        : dto.aliases().stream().map(a -> new ImageAlias(a.name(), new ImageId(dto.fingerprint()))))
                .toList();
    }

    private void runImport(UploadPayload payload, ImageImportHandle handle) {
        try (InputStream raw = payload.stream()) {
            InputStream counting = new CountingInputStream(raw, transferred -> handle.updateBytes(transferred, payload.contentLength()));
            String operationId = client.postStreamForOperationId("/images", counting);
            if (operationId == null) {
                handle.fail(new OperationException(
                        "Incus completed image import synchronously without returning a fingerprint"));
                return;
            }
            ImageOperationWaiter.waitImport(client, operationId, handle, operationWaitTimeout);
        } catch (IOException e) {
            handle.fail(new ConnectionException("Failed while uploading image", e));
        }
    }

    private UploadPayload openUploadSource(ImageSource source) {
        if (source instanceof LocalFileImageSource local) {
            try {
                return new UploadPayload(Files.newInputStream(local.path()), Files.size(local.path()));
            } catch (IOException e) {
                throw new ConnectionException("Failed to open image file " + local.path(), e);
            }
        }
        if (source instanceof InputStreamImageSource stream) {
            return new UploadPayload(stream.stream(), stream.contentLength());
        }
        throw new UnsupportedCapabilityException(
                "IncusImageProvider does not support importing from " + source.getClass().getSimpleName());
    }

    private String resolveFingerprint(String identifier) {
        try {
            return client.getSingle("/images/aliases/" + identifier, ImageAliasDto.class).target();
        } catch (ResourceNotFoundException aliasNotFound) {
            return identifier;
        }
    }

    private Image fetchImage(String fingerprint) {
        return IncusImageMapper.toImage(client.getSingle("/images/" + fingerprint, ImageDto.class));
    }

    private static String pullRequestBody(IncusRemote source, String alias) {
        try {
            return MAPPER.writeValueAsString(
                    new PullRequestDto(new PullSourceDto("image", "pull", source.server().toString(), source.protocol(), alias)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreachable: pull request body always serializes", e);
        }
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private record UploadPayload(InputStream stream, long contentLength) {}

    private record PullSourceDto(String type, String mode, String server, String protocol, String alias) {}

    private record PullRequestDto(PullSourceDto source) {}
}
