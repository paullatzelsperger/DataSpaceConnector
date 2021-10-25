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

package org.eclipse.dataspaceconnector.asset.index.azure;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.asset.index.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

@IntegrationTest
class CosmosAssetIndexIntegrationTest {

    public static final String REGION = "westeurope";
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String DATABASE_NAME = "connector-itest";
    private static final String CONTAINER_NAME = "CosmosAssetIndexTest";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private CosmosAssetIndex assetIndex;
    private TypeManager typeManager;

    @BeforeAll
    static void prepareCosmosClient() {
        var key = propOrEnv("COSMOS_KEY", null);
        if (key != null) {
            var client = new CosmosClientBuilder()
                    .key(key)
                    .preferredRegions(Collections.singletonList(REGION))
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .endpoint("https://" + ACCOUNT_NAME + ".documents.azure.com:443/")
                    .buildClient();

            CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
            database = client.getDatabase(response.getProperties().getId());
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        typeManager = new TypeManager();
        typeManager.registerTypes(Asset.class, AssetDocument.class);
        String partitionKey = "testpartition";
        assetIndex = new CosmosAssetIndex(container, partitionKey, typeManager, new RetryPolicy<>());
    }

    @Test
    void queryAssets_selectAll() {
        Asset asset1 = Asset.Builder.newInstance()
                .id("123")
                .property("hello", "world")
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id("456")
                .property("foo", "bar")
                .build();


        container.createItem(asset1);
        container.createItem(asset2);

        List<Asset> assets = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL).collect(Collectors.toList());

        assertThat(assets).hasSize(2);
        assertThat(assets.get(0).getProperties()).isEqualTo(asset1.getProperties());
        assertThat(assets.get(1).getProperties()).isEqualTo(asset2.getProperties());
    }

    @Test
    void queryAssets_filterOnProperty() {
        Asset asset1 = Asset.Builder.newInstance()
                .id("123")
                .property("test", "world")
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id("456")
                .property("test", "bar")
                .build();


        container.createItem(asset1);
        container.createItem(asset2);

        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("test", "bar")
                .build();

        List<Asset> assets = assetIndex.queryAssets(expression).collect(Collectors.toList());

        assertThat(assets).hasSize(1)
                .allSatisfy(asset -> {
                    assertThat(asset.getId()).isEqualTo("456");
                });
    }

    @Test
    void findById() {
        Asset asset1 = Asset.Builder.newInstance()
                .id("123")
                .property("test", "world")
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id("456")
                .property("test", "bar")
                .build();

        container.createItem(asset1);
        container.createItem(asset2);

        Asset asset = assetIndex.findById("456");

        assertThat(asset).isNotNull();
        assertThat(asset.getProperties()).isEqualTo(asset2.getProperties());
    }

    @AfterEach
    void teardown() {
        CosmosContainerResponse delete = container.delete();
        assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
    }

    private AssetDocument convert(Object obj) {
        return typeManager.readValue(typeManager.writeValueAsBytes(obj), AssetDocument.class);
    }
}
