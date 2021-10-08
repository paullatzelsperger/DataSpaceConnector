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

package org.eclipse.dataspaceconnector.federatedcatalogcache.memory;

import org.eclipse.dataspaceconnector.catalog.spi.AssetStore;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An ephemeral metadata store.
 */
public class InMemoryAssetStore implements AssetStore {

    private final Map<String, Asset> cache = new ConcurrentHashMap<>();

    @Override
    public @Nullable Asset findForId(String id) {
        return cache.get(id);
    }

    @Override
    public void save(Asset asset) {
        cache.put(asset.getId(), asset);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        return cache.values().stream()
                .filter(buildPredicate(expression));
    }

    private Predicate<Asset> buildPredicate(final AssetSelectorExpression assetSelectorExpression) {
        return buildPredicate(assetSelectorExpression.getFilterLabels());
    }

    private Predicate<Asset> buildPredicate(final Map<String, String> labels) {
        // bag for collecting all composable predicates
        final List<Predicate<Asset>> predicates = new LinkedList<>();

        predicates.add(new LabelsPredicate(Optional.ofNullable(labels).orElseGet(Collections::emptyMap)));

        // if example asset does not provide any meaningful properties this will
        // lead to true
        return predicates.stream().reduce(x -> true, Predicate::and);
    }

    private static class LabelsPredicate implements Predicate<Asset> {
        private final Map<String, String> labels;

        private LabelsPredicate(final Map<String, String> labels) {
            this.labels = labels;
        }

        @Override
        public boolean test(final Asset asset) {
            // iterate through all labels and check for equality
            // Note: map#equals not usable here!
            return labels.entrySet().stream().allMatch(kv -> asset.getLabels().containsKey(kv.getKey()) &&
                    kv.getValue().equals(asset.getLabels().get(kv.getKey())));
        }
    }
}
