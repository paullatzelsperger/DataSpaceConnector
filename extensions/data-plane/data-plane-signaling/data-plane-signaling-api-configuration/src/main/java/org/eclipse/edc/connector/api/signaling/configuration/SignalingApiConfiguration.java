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

package org.eclipse.edc.connector.api.signaling.configuration;

import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

/**
 * Signaling api configuration
 *
 * @deprecated ControlApiConfiguration should be used instead.
 */
@Deprecated(since = "0.6.4")
public class SignalingApiConfiguration extends WebServiceConfiguration {

    public SignalingApiConfiguration(String contextAlias) {
        super();
        this.contextAlias = contextAlias;
    }

    public SignalingApiConfiguration(WebServiceConfiguration webServiceConfiguration) {
        this.contextAlias = webServiceConfiguration.getContextAlias();
        this.path = webServiceConfiguration.getPath();
        this.port = webServiceConfiguration.getPort();
    }
}
