package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.tools.ElementTools;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.fields.FieldMethodExtractor;
import com.plsql.tools.tools.fields.info.FieldInfo;
import com.plsql.tools.tools.fields.info.ObjectInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.plsql.tools.tools.Tools.RETURN_NAME;
import static com.plsql.tools.tools.Tools.getTypeElement;

@Deprecated
public class MethodProcessor {
    private final ProcessingContext context;
    private final FieldMethodExtractor extractor;
    private final ElementTools elementTools;

    public MethodProcessor(ProcessingContext context) {
        this.context = context;
        this.extractor = FieldMethodExtractor.getInstance(context);
        this.elementTools = new ElementTools(context);
    }


    // TODO : handle no args and all args constructor
    // TODO : handle List stream and ResultSet ?
    public MethodProcessingResult process(ExecutableElement method) {
        var parameters = processParameters(method);
        return new MethodProcessingResult(parameters,
                processReturn(method),
                extractParamNames(parameters));
    }

    private VariableInfo processReturn(ExecutableElement method) {
        String returnType = method.getReturnType().toString();
        if (Tools.isVoid(returnType)) {
            return null;
        }

        var declaredType = (DeclaredType) method.getReturnType();
        var outputs = elementTools.extractAnnotationsFromReturn(method);

        context.logDebug("Extracted outputs : ", elementTools.extractAnnotationsFromReturn(method));

        if (JdbcHelper.fromSimpleName(declaredType.asElement().toString()) != null) {
            var returnVariable = new VariableInfo(declaredType.asElement());
            returnVariable.setOutputs(outputs);
            returnVariable.setCustomName(RETURN_NAME);
            return returnVariable;
        }

        var returnTypeElement = getTypeElement(context, declaredType.asElement());

        if (declaredType.getTypeArguments().isEmpty()) {
            validateOutputAnnotation(returnTypeElement);
            var objectInfo = extractor.extractFields(RETURN_NAME, (TypeElement) declaredType.asElement());
            objectInfo.setOutputs(outputs);
            return objectInfo;
        } else if (elementTools.isSimpleWrapper(declaredType.asElement())) { // TODO: do special handling for Map
            var argOpt = declaredType
                    .getTypeArguments()
                    .stream()
                    .findFirst();
            if (argOpt.isPresent()) {
                var type = Tools.getTypeElement(context, argOpt.get());
                validateOutputAnnotation(type);
                var objectInfo = extractor.extractFields(RETURN_NAME, type);
                objectInfo.setWrapper((TypeElement) declaredType.asElement());
                objectInfo.setOutputs(outputs);
                return objectInfo;
            }
        }
        return null;
    }

    private void validateOutputAnnotation(TypeElement type) {
        if (isTypeOutput(type) && isFieldOutput(type)) {
            // throw new IllegalStateException("@Output can be placed only on the class or only on the class fields");
        }
    }

    private boolean isTypeOutput(TypeElement typeElement) {
        return typeElement != null && typeElement.getAnnotation(Output.class) != null;
    }

    private boolean isFieldOutput(TypeElement typeElement) {
        return typeElement != null && extractor.extractClassInfo(typeElement)
                .stream()
                .anyMatch(fieldInfo -> fieldInfo.getField().getAnnotation(Output.class) != null);
    }

    private List<VariableInfo> processParameters(ExecutableElement method) {
        List<VariableInfo> variableInfoList = new ArrayList<>();
        for (var parameter : method.getParameters()) {
            var paramType = parameter.asType();
            var paramName = parameter.getSimpleName().toString();

            JdbcHelper type = JdbcHelper.fromSimpleName(paramType.toString());
            if (type != null) {
                // Simple parameter
                variableInfoList.add(new VariableInfo(parameter));
            } else {
                TypeElement typeElement = (TypeElement) context.getProcessingEnv().getTypeUtils().asElement(paramType);
                variableInfoList.add(extractor.extractFields(paramName, typeElement));
            }
        }
        return variableInfoList;
    }

    private List<String> extractParamNames(List<VariableInfo> variableInfoList) {
        List<String> params = new ArrayList<>();
        for (var variableInfo : variableInfoList) {
            if (variableInfo.isSimple()) {
                params.add(variableInfo.inputName());
            } else {
                var objectInfo = (ObjectInfo) variableInfo;
                extractNestedFields(objectInfo, objectInfo.getFieldInfoSet(), params);
            }
        }
        return params;
    }

    private void extractNestedFields(ObjectInfo objectInfo, Set<FieldInfo> fieldInfoSet, List<String> params) {
        for (var parentField : fieldInfoSet) {
            if (parentField.isSimple()) {
                params.add(parentField.inputName());
            } else {
                TypeElement typeElement = getTypeElement(context, parentField.getField());
                var nestedFields = objectInfo.getNestedObjects().get(typeElement);
                extractNestedFields(objectInfo, nestedFields, params);
            }
        }
    }
}
