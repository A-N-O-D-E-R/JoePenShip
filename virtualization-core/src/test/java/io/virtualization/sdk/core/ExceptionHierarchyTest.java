package io.virtualization.sdk.core;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.AuthorizationException;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHierarchyTest {

    @ParameterizedTest
    @ValueSource(classes = {
            AuthenticationException.class,
            AuthorizationException.class,
            ConnectionException.class,
            ResourceNotFoundException.class,
            OperationException.class,
            UnsupportedCapabilityException.class,
            ConfigurationException.class
    })
    void isUncheckedAndSupportsBothConstructors(Class<? extends VirtualizationException> type)
            throws ReflectiveOperationException {
        assertThat(VirtualizationException.class).isAssignableFrom(type);
        assertThat(RuntimeException.class).isAssignableFrom(type);

        VirtualizationException withMessage = instantiate(type, "boom");
        assertThat(withMessage).hasMessage("boom").hasNoCause();

        Throwable cause = new IllegalStateException("root cause");
        VirtualizationException withCause = instantiate(type, "boom", cause);
        assertThat(withCause).hasMessage("boom").hasCause(cause);
    }

    private static VirtualizationException instantiate(Class<? extends VirtualizationException> type, String message)
            throws ReflectiveOperationException {
        Constructor<? extends VirtualizationException> constructor = type.getConstructor(String.class);
        return invoke(constructor, message);
    }

    private static VirtualizationException instantiate(
            Class<? extends VirtualizationException> type, String message, Throwable cause)
            throws ReflectiveOperationException {
        Constructor<? extends VirtualizationException> constructor = type.getConstructor(String.class, Throwable.class);
        return invoke(constructor, message, cause);
    }

    private static VirtualizationException invoke(Constructor<? extends VirtualizationException> constructor, Object... args)
            throws ReflectiveOperationException {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        }
    }
}
