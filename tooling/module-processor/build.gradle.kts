/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       SAP SE - Minor fix
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":tooling:module-domain"))
    implementation("com.vladsch.flexmark:flexmark-all:0.64.0")
}

publishing {
    publications {
        create<MavenPublication>("module-processor") {
            artifactId = "module-processor"
            from(components["java"])
        }
    }
}
