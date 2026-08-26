package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.vps.CreateVpsOperation;

record CreateVpsView(OperationView operation, String vpsId) {

    static CreateVpsView from(CreateVpsOperation operation) {
        return new CreateVpsView(OperationView.from(operation), operation.vpsId().value());
    }
}
