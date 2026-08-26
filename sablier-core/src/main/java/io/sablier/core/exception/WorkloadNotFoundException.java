package io.sablier.core.exception;

/** Thrown when a workload group resolves to nothing, or a requested workload id does not exist. */
public class WorkloadNotFoundException extends SablierException {

    public WorkloadNotFoundException(String message) {
        super(message);
    }

    public WorkloadNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
