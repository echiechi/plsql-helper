package com.plsql.tools.statements.generators;

import com.plsql.tools.handlers.*;
import com.plsql.tools.statements.Generator;
import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.util.List;

import static com.plsql.tools.tools.CodeGenConstants.variableName;

public class ReturnGenerator implements Generator {

    private final List<ReturnElementInfo> returnElements;

    private final Extractor extractor;

    public ReturnGenerator(List<ReturnElementInfo> returnElements,
                           Extractor extractor) {
        this.returnElements = returnElements;
        this.extractor = extractor;
    }

    @Override
    public String generate() {
        if (returnElements == null) {
            return "";
        }
        boolean isMultiOutput = returnElements.size() > 1;
        StringBuilder sb = new StringBuilder();
        for (int i = returnElements.size() - 1; i >= 0; i--) {
            var returnElement = returnElements.get(i);
            if (i != returnElements.size() - 1) {
                returnElement.setPos(GenTools.preDecrementVar(returnElement.getPos()));
            }
            if (returnElement.hasRedundantType()) {
                throw new IllegalStateException("Redundant type is not yet supported (the usage of the same class type twice in a return type)");
            }
            ReturnTypeHandler returnTypeHandler = selectHandler(returnElement);
            sb.append(returnTypeHandler.generateCode(returnElement)).append("\n");
        }
        // multi output
        if (isMultiOutput) {
            var parent = returnElements.get(0).getParent();
            sb.append(GenTools.initObject(parent))
                    .append("\n");
            extractor
                    .getAttachedElements(parent.getTypeInfo().getMirror())
                    .forEach(a -> {
                        var setter = GenTools.constructMethod(variableName(parent.getName()),
                                a.getSetter().getSimpleName().toString(),
                                variableName(a.getName())
                        ).concat(";");
                        sb.append(setter).append("\n");
                    });
        }
        return sb.toString();
    }

    private ReturnTypeHandler selectHandler(ReturnElementInfo returnElement) {
        var returnTypeDetector = new ReturnTypeDetector(extractor);
        return switch (returnTypeDetector.categorize(returnElement)) {
            case SIMPLE -> new SimpleReturnHandler();
            case COMPOSED -> new ComposedReturnHandler(extractor);
            case OPTIONAL_SIMPLE, OPTIONAL_COMPOSED -> new OptionalReturnHandler(extractor);
            case COLLECTION -> new CollectionReturnHandler(extractor);
        };
    }

}
