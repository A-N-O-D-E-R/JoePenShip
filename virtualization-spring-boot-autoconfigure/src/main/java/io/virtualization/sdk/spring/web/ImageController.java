package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageDownload;
import io.virtualization.sdk.core.image.ImageImportOperation;
import io.virtualization.sdk.core.image.ImagePullOperation;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ImageType;
import io.virtualization.sdk.core.image.InputStreamImageSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * {@code /api/v1/images} — thin HTTP facade over {@link io.virtualization.sdk.core.image.ImageProvider}.
 * {@code provider} selects which configured {@link VirtualizationClient#images} entry to use, the
 * same role {@code --provider} plays in the CLI.
 */
@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final VirtualizationClient client;

    public ImageController(VirtualizationClient client) {
        this.client = client;
    }

    @GetMapping
    public List<Image> list(@RequestParam String provider) {
        return client.images(provider).list();
    }

    @GetMapping("/search")
    public List<Image> search(
            @RequestParam String provider,
            @RequestParam(name = "q", required = false) String name,
            @RequestParam(required = false) String distribution,
            @RequestParam(required = false) String architecture,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) ImageType type,
            @RequestParam(required = false) String remote) {
        ImageQuery.Builder builder = ImageQuery.builder();
        if (name != null) {
            builder.name(name);
        }
        if (distribution != null) {
            builder.distribution(distribution);
        }
        if (architecture != null) {
            builder.architecture(architecture);
        }
        if (os != null) {
            builder.operatingSystem(os);
        }
        if (version != null) {
            builder.version(version);
        }
        if (type != null) {
            builder.type(type);
        }
        if (remote != null) {
            builder.remote(remote);
        }
        return client.images(provider).search(builder.build());
    }

    // {*id} rather than {id}: an id like "images:ubuntu/24.04" contains a literal '/'. A plain
    // {id} single-segment variable won't match it, percent-encoding it as %2F is rejected by
    // embedded Tomcat by default, and — contrary to the old AntPathMatcher — Spring's PathPattern
    // matcher (the default since Spring 5) never lets a {id:.+}-style regex variable span
    // multiple '/'-segments; the regex only constrains what's allowed within one segment. {*id}
    // is the dedicated multi-segment "rest of the path" capture syntax, and must be the last
    // element of the pattern; it captures the leading '/' too, hence the substring(1) below.
    @GetMapping("/{*id}")
    public Image get(@RequestParam String provider, @PathVariable String id) {
        ImageReference reference = ImageReferences.parse(provider, id.substring(1));
        return client.images(provider).get(reference)
                .orElseThrow(() -> new ResourceNotFoundException("No image '" + id + "' on provider '" + provider + "'"));
    }

    // /download/{*id}, not /{*id}/download: {*id} must be the terminal element of the pattern.
    @GetMapping("/download/{*id}")
    public void download(@RequestParam String provider, @PathVariable String id, HttpServletResponse response) throws IOException {
        ImageReference reference = ImageReferences.parse(provider, id.substring(1));
        try (ImageDownload download = client.images(provider).download(reference)) {
            response.setContentType(download.mediaType().orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE));
            download.contentLength().ifPresent(response::setContentLengthLong);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + sanitizeFilename(id) + "\"");
            download.stream().transferTo(response.getOutputStream());
        }
    }

    @PostMapping("/pull")
    public OperationView pull(@RequestBody PullRequestBody request) {
        ImageReference reference = new ImageReference(request.provider(), request.remote(), request.identifier());
        ImagePullOperation operation = client.images(request.provider()).pull(reference);
        operation.await(WebDefaults.OPERATION_TIMEOUT);
        return OperationView.from(operation);
    }

    /** Streams the raw request body straight into the provider — never buffered in full. */
    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ImageImportView importImage(@RequestParam String provider, HttpServletRequest request) throws IOException {
        long contentLength = request.getContentLengthLong();
        ImageImportOperation operation =
                client.images(provider).importImage(new InputStreamImageSource(request.getInputStream(), contentLength));
        operation.await(WebDefaults.OPERATION_TIMEOUT);
        return new ImageImportView(OperationView.from(operation), operation.result().orElse(null));
    }

    /**
     * {@code id} is untrusted network input and may contain path separators (e.g.
     * {@code "images:ubuntu/24.04"}); strip anything that isn't safe in a {@code
     * Content-Disposition} filename to avoid header injection or a misleading save-as path.
     */
    private static String sanitizeFilename(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }
}
