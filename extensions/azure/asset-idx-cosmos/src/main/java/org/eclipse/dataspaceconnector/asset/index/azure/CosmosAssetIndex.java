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

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.asset.index.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.Objects;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosAssetIndex implements AssetIndex {

    private final CosmosContainer container;
    private final CosmosQueryRequestOptions tracingOptions;
    private final TypeManager typeManager;
    private final String partitionKey;
    private final RetryPolicy<Object> retryPolicy;

    /**
     * Creates a new instance of the CosmosDB-based for Asset storage.
     *
     * @param container   The CosmosDB container.
     * @param typeManager The {@link TypeManager} that's used for serialization and deserialization
     */
    public CosmosAssetIndex(CosmosContainer container, String partitionKey, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.container = container;
        this.typeManager = typeManager;
        this.partitionKey = partitionKey;
        tracingOptions = new CosmosQueryRequestOptions();
        tracingOptions.setQueryMetricsEnabled(true);
        this.retryPolicy = retryPolicy;
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        Objects.requireNonNull(expression, "AssetSelectorExpression can not be null!");

        String query = CosmosAssetQueryBuilder.from(expression);
        try {
            var response = with(retryPolicy).get(() -> container.queryItems(query, tracingOptions, Object.class));
            return response.stream()
                    .map(this::convertObject)
                    .map(AssetDocument::getWrappedInstance);
        } catch (CosmosException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public Asset findById(String assetId) {
        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        try {
            // we need to read the AssetDocument as Object, because no custom JSON deserialization can be registered
            // with the CosmosDB SDK, so it would not know about subtypes, etc.
            CosmosItemResponse<Object> response = with(retryPolicy).get(() -> container.readItem(assetId, new PartitionKey(partitionKey), options, Object.class));
            var obj = response.getItem();

            return convertObject(obj).getWrappedInstance();
        } catch (NotFoundException ex) {
            return null;
        }
    }

    private AssetDocument convertObject(Object databaseDocument) {
        return typeManager.readValue(typeManager.writeValueAsString(databaseDocument), AssetDocument.class);
    }
}
