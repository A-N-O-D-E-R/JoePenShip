package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

/**
 * A backend capable of listing, inspecting, searching, pulling, downloading and importing images
 * (Incus, Docker, Podman, ...).
 *
 * <p>Not every provider supports every operation; check {@link #capabilities()} before invoking
 * one. {@code pull}/{@code download}/{@code importImage} default to throwing {@link
 * UnsupportedCapabilityException} so a provider that only supports listing does not need to
 * override them. Instantiate-from-image is added on top of this interface in a later phase.
 */
public interface ImageProvider {

    String name();

    ImageCapabilities capabilities();

    List<Image> list();

    Optional<Image> get(ImageReference reference);

    List<Image> search(ImageQuery query);

    /**
     * Retrieves an image from a remote and makes it available to this provider (e.g. into the
     * local Incus image store). Distinct from {@link #download(ImageReference)}, which exports
     * image data to the caller instead of pulling it into the provider's own store.
     *
     * @throws UnsupportedCapabilityException if this provider does not support pulling
     */
    default ImagePullOperation pull(ImageReference reference) {
        throw new UnsupportedCapabilityException(name() + " does not support pulling images");
    }

    /**
     * Exports an image as a stream of data. The caller must close the returned {@link
     * ImageDownload}, which closes the underlying stream.
     *
     * @throws UnsupportedCapabilityException if this provider does not support downloading
     */
    default ImageDownload download(ImageReference reference) {
        throw new UnsupportedCapabilityException(name() + " does not support downloading images");
    }

    /** Convenience over {@link #download(ImageReference)} that streams directly into {@code out}. */
    default void download(ImageReference reference, OutputStream out) {
        try (ImageDownload download = download(reference)) {
            download.stream().transferTo(out);
        } catch (IOException e) {
            throw new ConnectionException("Failed to stream image download for " + reference, e);
        }
    }

    /**
     * Imports an image from the given source, making it available to this provider.
     *
     * @throws UnsupportedCapabilityException if this provider does not support importing
     */
    default ImageImportOperation importImage(ImageSource source) {
        throw new UnsupportedCapabilityException(name() + " does not support importing images");
    }
}
