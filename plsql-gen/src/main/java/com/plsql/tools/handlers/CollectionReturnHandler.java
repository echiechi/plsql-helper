package com.plsql.tools.handlers;

import com.plsql.tools.tools.GenTools;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import javax.lang.model.type.TypeMirror;

import static com.plsql.tools.tools.CodeGenConstants.variableName;
import static com.plsql.tools.tools.CodeGenConstants.wrappedVariableName;
import static com.plsql.tools.tools.Tools.defaultInitTypeForList;

public class CollectionReturnHandler implements ReturnTypeHandler {

    private final Extractor extractor;

    public CollectionReturnHandler(Extractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public boolean canHandle(ReturnElementInfo returnElement) {
        return returnElement.getTypeInfo().isWrapped() &&
                extractor.isCollection(returnElement.getTypeInfo().getMirror());
    }

    @Override
    public String generateCode(ReturnElementInfo returnElement) {
        var defaultReturnName = returnElement.getName();
        var wrappedVariableName = wrappedVariableName(defaultReturnName);
        var addObjectToList = GenTools.addToCollection(
                variableName(defaultReturnName),
                variableName(wrappedVariableName));

        ComposedReturnHandler composedReturnHandler = new ComposedReturnHandler(
                extractor,
                false,
                true,
                false,
                false,
                addObjectToList);

        TypeMirror listType = extractor.eraseType(returnElement.getTypeInfo().getMirror());
        // init list/set
        var listInit = GenTools.collectionInit(
                listType.toString(),
                returnElement.getTypeInfo().wrappedTypeAsString(),
                variableName(defaultReturnName),
                defaultInitTypeForList(listType)
        );
        return listInit + "\n" + composedReturnHandler
                .generateCode(returnElement);
    }
}
