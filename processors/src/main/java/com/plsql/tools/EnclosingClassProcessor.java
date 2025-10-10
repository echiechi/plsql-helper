package com.plsql.tools;

import com.plsql.tools.annotations.Function;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.statement.generators.FunctionMethodGenerator;
import com.plsql.tools.statement.generators.ProcedureMethodGenerator;
import com.plsql.tools.templates.TemplateParams;
import com.plsql.tools.templates.Templates;
import com.plsql.tools.tools.Tools;
import org.stringtemplate.v4.ST;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnclosingClassProcessor {
    private final ProcessingContext context;
    private final TypeElement packageClass;
    private final List<String> generatedMethods;
    private final Set<String> processedMethods;

    public EnclosingClassProcessor(ProcessingContext context, TypeElement packageClass) {
        this.context = context;
        this.packageClass = packageClass;
        this.generatedMethods = new ArrayList<>();
        this.processedMethods = new HashSet<>();
    }

    public String generateImplementation() {
        processMethods();
        return generateClass();
    }

    private void processMethods() {
        List<ExecutableElement> methods = packageClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .toList();

        for (ExecutableElement method : methods) {
            context.logInfo("Generating implementation for:", method);
            processMethod(method);
        }
    }

    private void processMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (processedMethods.contains(methodName)) { // should not be possible ?
            context.logError("Duplicate method name found: " + methodName);
            return;
        }

        try {
            if (method.getAnnotation(Procedure.class) != null) {
                String procedureMethod = generateProcedureCall(method);
                if (procedureMethod != null) {
                    generatedMethods.add(procedureMethod);
                    processedMethods.add(methodName);
                }
            } else if (method.getAnnotation(Function.class) != null) {
                String functionMethod = generateFunctionCall(method);
                if (functionMethod != null) {
                    generatedMethods.add(functionMethod);
                    processedMethods.add(methodName);
                }
            }
        } catch (Exception e) {
            context.logError("Error processing method " + methodName + ": " + e.getMessage());
        }
    }

    private String generateFunctionCall(ExecutableElement method) {
        try {
            FunctionMethodGenerator generator = new FunctionMethodGenerator(
                    context, packageClass, method
            );
            return generator.generate();
        } catch (Exception e) {
            context.logError("Failed to generate function call for " + method.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    private String generateProcedureCall(ExecutableElement method) {
        try {
            ProcedureMethodGenerator generator = new ProcedureMethodGenerator(
                    context, packageClass, method);
            return generator.generate();
        } catch (Exception e) {
            context.logError("Failed to generate procedure call for " + method.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    private String generateClass() {
        String originalClassName = packageClass.getSimpleName().toString();
        String generatedClassName = originalClassName + "Impl";
        String packageName = Tools.getPackageNameSafe((TypeElement) packageClass).orElse("");

        ST templateBuilder = new ST(Templates.CLASS_TEMPLATE);
        templateBuilder.add(TemplateParams.PACKAGE_NAME.name(), packageName);
        templateBuilder.add(TemplateParams.CLASS_NAME.name(), generatedClassName);
        templateBuilder.add(TemplateParams.EXTENDED_CLASS_NAME.name(), originalClassName);
        templateBuilder.add(TemplateParams.METHODS.name(), generatedMethods);

        return templateBuilder.render();
    }

}
