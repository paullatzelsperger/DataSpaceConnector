/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val jupiterVersion: String by project

dependencies {
    implementation(project(":core"))
    implementation(project(":data-protocols:ids"))
    implementation(project(":extensions:azure:blob"))
    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:policy-registry-memory"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    //FIXME for testing only: spin up a runtime that contains an FCC
    implementation(project(":extensions:catalog:federated-catalog-cache"))
    implementation(project(":extensions:in-memory:node-directory-memory"))
    implementation(project(":extensions:in-memory:fcc-protocol-registry-memory"))
}

application {
    @Suppress("DEPRECATION")
    mainClassName = "org.eclipse.dataspaceconnector.system.runtime.BaseRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dataspaceconnector-basic.jar")
}
