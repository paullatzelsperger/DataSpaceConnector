package org.eclipse.dataspaceconnector.asset.index.azure;

import org.eclipse.dataspaceconnector.asset.index.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;

import java.util.StringJoiner;

public class CosmosAssetQueryBuilder {

    private static final String NAME = AssetDocument.class.getSimpleName();

    private CosmosAssetQueryBuilder() {
    }

    public static String from(AssetSelectorExpression expression) {
        String query = "SELECT * FROM " + NAME;
        if (expression != AssetSelectorExpression.SELECT_ALL) {
            query += buildWhereClause(expression);
        }
        return query;
    }

    private static String buildWhereClause(AssetSelectorExpression expression) {
        StringJoiner joiner = new StringJoiner(" AND ");
        expression.getCriteria().stream()
                .map(CosmosAssetQueryBuilder::toSql)
                .forEach(joiner::add);
        return " WHERE " + joiner.toString();
    }

    private static String toSql(Criterion criterion) {
        return "( AssetDocument.wrappedInstance.properties." + criterion.getOperandLeft() +
                " " + criterion.getOperator() + " " +
                criterion.getOperandRight() + " )";
    }
}
