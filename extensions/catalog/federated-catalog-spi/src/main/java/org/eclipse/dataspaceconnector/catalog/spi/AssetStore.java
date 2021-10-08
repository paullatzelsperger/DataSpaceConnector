package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

/**
 * Delegates to a storage system for the persistence and management of catalog entries.
 */
public interface AssetStore extends AssetIndex {

    String FEATURE = "edc:federated-catalog-cache:store";

    /**
     * Returns the entry for the id or null if not found.
     */
    @Nullable
    Asset findForId(String id);

    /**
     * Saves an asset to the backing store.
     */
    void save(Asset asset);

}
