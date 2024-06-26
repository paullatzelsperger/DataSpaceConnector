/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.control.api;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.dataplane.selector.control.api.model.SelectionRequest;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.time.Clock;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/dataplanes")
public class DataplaneSelectorControlApiController implements DataplaneSelectorControlApi {

    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final DataPlaneSelectorService service;
    private final Clock clock;

    public DataplaneSelectorControlApiController(JsonObjectValidatorRegistry validatorRegistry,
                                                 TypeTransformerRegistry transformerRegistry,
                                                 DataPlaneSelectorService service, Clock clock) {

        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.clock = clock;
    }

    @Override
    @POST
    public JsonObject registerDataplane(JsonObject request) {
        validatorRegistry.validate(DataPlaneInstance.DATAPLANE_INSTANCE_TYPE, request).orElseThrow(ValidationFailureException::new);

        var dataplane = transformerRegistry.transform(request, DataPlaneInstance.class).orElseThrow(InvalidRequestException::new);

        service.addInstance(dataplane)
                .orElseThrow(exceptionMapper(DataPlaneInstance.class, dataplane.getId()));

        var idResponse = IdResponse.Builder.newInstance()
                .id(dataplane.getId())
                .createdAt(clock.millis())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @Override
    @DELETE
    @Path("/{id}")
    public void unregisterDataplane(@PathParam("id") String id) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @POST
    @Path("/select")
    @Override
    public JsonObject selectDataplane(JsonObject request) {
        var selectionRequest = transformerRegistry.transform(request, SelectionRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var dataPlaneInstance = service.select(selectionRequest.getSource(), selectionRequest.getTransferType(), selectionRequest.getStrategy())
                .orElseThrow(exceptionMapper(DataPlaneInstance.class));

        return transformerRegistry.transform(dataPlaneInstance, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @Override
    public JsonArray getAllDataPlaneInstances() {
        var instances = service.getAll().orElseThrow(exceptionMapper(DataPlaneInstance.class));

        return instances.stream()
                .map(i -> transformerRegistry.transform(i, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

}
