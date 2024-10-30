/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.transferprocess.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;

/**
 * Validator for {@link TransferTerminationMessage} Json-LD representation
 */
public class TransferTerminationMessageValidator {

    public static Validator<JsonObject> instance() {
        return instance(DSP_NAMESPACE_V_08);
    }

    public static Validator<JsonObject> instance(JsonLdNamespace namespace) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new TypeIs(path, namespace.toIri(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM)))
                .build();
    }
}