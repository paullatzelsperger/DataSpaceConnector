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

package org.eclipse.edc.protocol.dsp.negotiation.http.api.validation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS;

class ContractRequestMessageValidatorTest {

    private final Validator<JsonObject> validator = ContractRequestMessageValidator.instance();

    @Test
    void shouldSucceed_whenObjectIsValid() {
        var input = createObjectBuilder()
                .add(TYPE, createArrayBuilder().add(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE))
                .add(DSPACE_PROPERTY_OFFER, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, ODRL_POLICY_TYPE_OFFER)
                                .add(ID, UUID.randomUUID().toString())
                                .add(ODRL_TARGET_ATTRIBUTE, id("target"))))
                .add(DSPACE_PROPERTY_CALLBACK_ADDRESS, value("http://any/address"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryFieldsAreMissing() {
        var input = createObjectBuilder()
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .hasSize(2)
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(TYPE))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(DSPACE_PROPERTY_CALLBACK_ADDRESS));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }

    @Test
    void shouldFail_whenOfferMissesIdAndTarget() {
        var input = createObjectBuilder()
                .add(TYPE, createArrayBuilder().add(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE))
                .add(DSPACE_PROPERTY_OFFER, createArrayBuilder().add(createObjectBuilder()))
                .add(DSPACE_PROPERTY_CALLBACK_ADDRESS, value("http://any/address"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .hasSize(2)
                .allSatisfy(violation -> assertThat(violation.path()).startsWith(DSPACE_PROPERTY_OFFER))
                .anySatisfy(violation -> assertThat(violation.path()).endsWith(ID))
                .anySatisfy(violation -> assertThat(violation.path()).endsWith(ODRL_TARGET_ATTRIBUTE));
    }

    private JsonArrayBuilder id(String id) {
        return createArrayBuilder().add(createObjectBuilder().add(ID, id));
    }

}
