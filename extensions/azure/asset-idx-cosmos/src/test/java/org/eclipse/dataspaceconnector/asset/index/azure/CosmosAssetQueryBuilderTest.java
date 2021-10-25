package org.eclipse.dataspaceconnector.asset.index.azure;

import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CosmosAssetQueryBuilderTest {

    @Test
    void queryAll() {
        String query = CosmosAssetQueryBuilder.from(AssetSelectorExpression.SELECT_ALL);

        assertThat(query).isEqualTo("SELECT * FROM AssetDocument");
    }

    @Test
    void queryAllWithCriteria() {
        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("id", "id-test")
                .whenEquals("name", "name-test")
                .build();

        String query = CosmosAssetQueryBuilder.from(expression);

        assertThat(query).isEqualTo("SELECT * FROM AssetDocument WHERE ( AssetDocument.wrappedInstance.properties.id = id-test ) AND ( AssetDocument.wrappedInstance.properties.name = name-test )");
    }
}