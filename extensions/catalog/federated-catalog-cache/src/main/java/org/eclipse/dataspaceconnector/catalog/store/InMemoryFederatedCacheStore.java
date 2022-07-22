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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An ephemeral in-memory cache store.
 */
public class InMemoryFederatedCacheStore implements FederatedCacheStore {

    private final Map<String, MarkableEntry> cache = new ConcurrentHashMap<>();
    private final CriterionConverter<Predicate<ContractOffer>> converter;
    private final LockManager lockManager;

    public InMemoryFederatedCacheStore(CriterionConverter<Predicate<ContractOffer>> converter, LockManager lockManager) {
        this.converter = converter;
        this.lockManager = lockManager;
    }

    @Override
    public void save(ContractOffer contractOffer) {
        lockManager.writeLock(() -> cache.put(contractOffer.getAsset().getId(), new MarkableEntry(false, contractOffer)));
    }

    @Override
    public Collection<ContractOffer> query(List<Criterion> query) {
        //AND all predicates
        var rootPredicate = query.stream().map(converter::convert).reduce(x -> true, Predicate::and);
        return lockManager.readLock(() -> cache.values().stream().map(MarkableEntry::getEntry).filter(rootPredicate).collect(Collectors.toList()));
    }

    @Override
    public void deleteExpired() {
        lockManager.writeLock(() -> {
            var unmarkedItems = cache.entrySet().stream().filter(entry -> !entry.getValue().isMarked())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            cache.keySet().retainAll(unmarkedItems);
            return null;
        });
    }

    @Override
    public void expireAll() {
        cache.replaceAll((k, v) -> v = new MarkableEntry(true, v.getEntry()));
    }

    private static class MarkableEntry {
        private final ContractOffer entry;
        private final boolean mark;

        MarkableEntry(boolean isMarked, ContractOffer offer) {
            entry = offer;
            mark = isMarked;
        }


        public boolean isMarked() {
            return mark;
        }

        public ContractOffer getEntry() {
            return entry;
        }

    }
}
