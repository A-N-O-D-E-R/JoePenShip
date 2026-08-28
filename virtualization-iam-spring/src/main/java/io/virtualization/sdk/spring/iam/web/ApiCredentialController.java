package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.ApiCredentialId;
import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.UserId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** {@code /api/v1/iam/credentials} — over whichever {@link ApiCredentialManager} bean {@code virtualization.iam.enabled=true} activates. */
@RestController
@RequestMapping("/api/v1/iam/credentials")
public class ApiCredentialController {

    private final ApiCredentialManager apiCredentialManager;

    public ApiCredentialController(ApiCredentialManager apiCredentialManager) {
        this.apiCredentialManager = apiCredentialManager;
    }

    @PostMapping
    public IssuedApiCredentialView issue(@RequestBody IssueApiCredentialRequestBody request) {
        Objects.requireNonNull(request.ownerId(), "'ownerId' is required");
        Objects.requireNonNull(request.name(), "'name' is required");
        Duration ttl = request.ttlSeconds() != null ? Duration.ofSeconds(request.ttlSeconds()) : null;
        return IssuedApiCredentialView.from(apiCredentialManager.issue(new UserId(request.ownerId()), request.name(), ttl));
    }

    @GetMapping
    public List<ApiCredentialView> list(@RequestParam String ownerId) {
        return apiCredentialManager.list(new UserId(ownerId)).stream().map(ApiCredentialView::from).toList();
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable String id) {
        apiCredentialManager.revoke(new ApiCredentialId(id));
    }
}
