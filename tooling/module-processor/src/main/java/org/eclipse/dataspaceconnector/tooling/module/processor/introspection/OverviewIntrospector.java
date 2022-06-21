/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.tooling.module.processor.introspection;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.eclipse.dataspaceconnector.spi.system.Extension;
import org.eclipse.dataspaceconnector.spi.system.SPI;
import org.eclipse.dataspaceconnector.tooling.module.domain.ModuleType;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;

/**
 * Generates a module overview.
 */
public class OverviewIntrospector {
    private Elements elementUtils;

    public OverviewIntrospector(Elements elementUtils) {
        this.elementUtils = elementUtils;
    }

    /**
     * Generated overview documentation by converting Javadoc to a Markdown representation. For SPI modules, the Javadoc is taken from the <code>package-info.java</code> type
     * annotated with {@link SPI}. For extensions, the Javadoc is taken from the type annotated with {@link Extension}.
     */
    @Nullable
    public String generateModuleOverview(ModuleType moduleType, RoundEnvironment environment) {
        var annotation = moduleType == ModuleType.EXTENSION ? Extension.class : SPI.class;
        var elements = environment.getElementsAnnotatedWith(annotation);
        if (elements.isEmpty()) {
            return null;
        }

        var moduleElement = elements.iterator().next();
        var javadoc = elementUtils.getDocComment(moduleElement);
        return javadoc != null ? FlexmarkHtmlConverter.builder().build().convert(javadoc) : "No overview provided.";
    }


}
