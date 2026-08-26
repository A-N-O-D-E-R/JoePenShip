package io.sablier.core;

import io.sablier.core.exception.AuthenticationException;
import io.sablier.core.exception.AuthorizationException;
import io.sablier.core.exception.ConfigurationException;
import io.sablier.core.exception.ConnectionException;
import io.sablier.core.exception.OperationException;
import io.sablier.core.exception.ResourceNotFoundException;
import io.sablier.core.exception.SablierException;
import io.sablier.core.exception.SessionNotFoundException;
import io.sablier.core.exception.WorkloadNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHierarchyTest {

    @ParameterizedTest
    @ValueSource(classes = {
            WorkloadNotFoundException.class,
            SessionNotFoundException.class,
            OperationException.class,
            ConnectionException.class,
            AuthenticationException.class,
            AuthorizationException.class,
            ConfigurationException.class,
            ResourceNotFoundException.class
    })
    void isUncheckedAndSupportsBothConstructors(Class<? extends SablierException> type) throws ReflectiveOperationException {
        assertThat(SablierException.class).isAssignableFrom(type);
        assertThat(RuntimeException.class).isAssignableFrom(type);

        SablierException withMessage = instantiate(type, "boom");
        assertThat(withMessage).hasMessage("boom").hasNoCause();

        Throwable cause = new IllegalStateException("root cause");
        SablierException withCause = instantiate(type, "boom", cause);
        assertThat(withCause).hasMessage("boom").hasCause(cause);
    }

    private static SablierException instantiate(Class<? extends SablierException> type, String message)
            throws ReflectiveOperationException {
        Constructor<? extends SablierException> constructor = type.getConstructor(String.class);
        return invoke(constructor, message);
    }

    private static SablierException instantiate(Class<? extends SablierException> type, String message, Throwable cause)
            throws ReflectiveOperationException {
        Constructor<? extends SablierException> constructor = type.getConstructor(String.class, Throwable.class);
        return invoke(constructor, message, cause);
    }

    private static SablierException invoke(Constructor<? extends SablierException> constructor, Object... args)
            throws ReflectiveOperationException {
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        }
    }
}
