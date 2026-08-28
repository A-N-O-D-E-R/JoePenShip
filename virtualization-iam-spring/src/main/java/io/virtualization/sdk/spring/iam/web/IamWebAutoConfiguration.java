package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.GroupManager;
import io.virtualization.sdk.iam.RoleManager;
import io.virtualization.sdk.iam.UserManager;
import io.virtualization.sdk.spring.iam.IamAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * {@code /api/v1/iam/*} — user/role/group/API-credential management, over whichever managers
 * {@code virtualization.iam.enabled=true} activates (see {@code IamAutoConfiguration}). Only
 * registers in a servlet web application, the same stance {@code
 * VirtualizationWebAutoConfiguration} takes for the base SDK's own REST controllers.
 */
@AutoConfiguration(after = IamAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "virtualization.iam", name = "enabled", havingValue = "true")
public class IamWebAutoConfiguration {

    @Bean
    @ConditionalOnBean(UserManager.class)
    public UserController userController(UserManager userManager) {
        return new UserController(userManager);
    }

    @Bean
    @ConditionalOnBean(RoleManager.class)
    public RoleController roleController(RoleManager roleManager) {
        return new RoleController(roleManager);
    }

    @Bean
    @ConditionalOnBean(GroupManager.class)
    public GroupController groupController(GroupManager groupManager) {
        return new GroupController(groupManager);
    }

    @Bean
    @ConditionalOnBean(ApiCredentialManager.class)
    public ApiCredentialController apiCredentialController(ApiCredentialManager apiCredentialManager) {
        return new ApiCredentialController(apiCredentialManager);
    }
}
