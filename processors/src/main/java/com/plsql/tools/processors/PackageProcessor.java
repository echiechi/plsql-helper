package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.TemplateParams;
import com.plsql.tools.Templates;
import com.plsql.tools.annotations.Function;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.tools.Tools;
import org.stringtemplate.v4.ST;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PackageProcessor {
    private final ProcessingContext context;
    private final Element packageClass;
    private final RoundEnvironment roundEnv;
    private final Package packageAnnotation;
    private final List<String> generatedMethods;
    private final Set<String> processedMethods;

    public PackageProcessor(ProcessingContext context, Element packageClass, RoundEnvironment roundEnv) {
        this.context = context;
        this.packageClass = packageClass;
        this.roundEnv = roundEnv;
        this.packageAnnotation = packageClass.getAnnotation(Package.class);
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
            processMethod(method);
        }
    }

    private void processMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();

        if (processedMethods.contains(methodName)) {
            context.logWarning("Duplicate method name found: " + methodName);
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
                context.logInfo("Function generation not yet implemented for: " + methodName);
            }
        } catch (Exception e) {
            context.logError("Error processing method " + methodName + ": " + e.getMessage());
        }
    }

    private String generateProcedureCall(ExecutableElement method) {
        Procedure methodAnnotation = method.getAnnotation(Procedure.class);

        try {
            ProcedureMethodGenerator generator = new ProcedureMethodGenerator(
                    context, roundEnv, method, packageAnnotation, methodAnnotation);
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
