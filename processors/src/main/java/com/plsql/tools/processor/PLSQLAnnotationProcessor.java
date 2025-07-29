package com.plsql.tools.processor;


import com.plsql.tools.annotations.Function;
import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Procedure;
import com.plsql.tools.processor.mapping.ObjectField;
import com.plsql.tools.processor.sql.ProcedureExecutionStatement;
import com.plsql.tools.utils.CaseConverter;
import org.stringtemplate.v4.ST;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.plsql.tools.processor.ProcessorTools.*;

@SupportedAnnotationTypes({
        "com.plsql.tools.annotations.PLSQLService",
        "com.plsql.tools.annotations.Procedure",
        "com.plsql.tools.annotations.Function",
        "com.plsql.tools.annotations.IN",
        "com.plsql.tools.annotations.OUT"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class PLSQLAnnotationProcessor extends AbstractProcessor {
    private Filer filer; // Used to create new source files

    // TODO : handle blob types for inputs/ outputs
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        kindMessage(processingEnv, "PL/SQL Annotation Processor initialized");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        kindMessage(processingEnv, "PL/SQL Started Processing");
        for (Element packageClass : roundEnv.getElementsAnnotatedWith(Package.class)) {
            if (packageClass.getKind() != ElementKind.CLASS) {
                errorMessage(processingEnv, "@Package can only be applied to classes " + packageClass.getSimpleName());
                continue;
            }
            //  String packageName = CaseConverter.toSnakeCase(packageClass.getSimpleName().toString());
            List<String> generatedMethods = new ArrayList<>();
            for (Element packageMember : packageClass.getEnclosedElements()) {
                if (packageMember.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) packageMember;
                    if (method.getAnnotation(Procedure.class) != null) {
                        generatedMethods.add(generateMethod(roundEnv, method, method.getAnnotation(Procedure.class)));
                    } else if (method.getAnnotation(Function.class) != null) {
                        generatedMethods.add(generateMethod(roundEnv, method, null));
                    }
                }
            }
            String originalClassName = packageClass.getSimpleName().toString();
            String generatedClassName = originalClassName + "Impl";
            String packageName = getPackageName((TypeElement) packageClass);
            JavaFileObject builderFile = null;
            try {
                builderFile = filer.createSourceFile(
                        packageName + "." + generatedClassName);
                try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                    out.print(generateClass((TypeElement) packageClass, generatedMethods));
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private String generateMethod(RoundEnvironment roundEnv, ExecutableElement method, Procedure procedure) {
        String returnType = method.getReturnType().toString();
        String methodName = method.getSimpleName().toString();
        String parameters =
                method.getParameters()
                        .stream()
                        .map(p -> String.format("%s %s", p.asType(), p.getSimpleName()))
                        .collect(Collectors.joining(","));

        Map<ObjectField, Element> extractedMap = new LinkedHashMap<>();
        method.getParameters().forEach(p -> { // TODO: test duplicate input objects or add the parameter name as prefix ?
            kindMessage(processingEnv, "--->" + p.getSimpleName());
            // TODO: 2 methods of extraction ( from object + from list of parameters ) or leave only one option
            var paramClass = findElementByName(roundEnv, p.asType().toString());
            if (paramClass == null) {
                errorMessage(processingEnv, "Can't find parameters class " + p.asType());
            } else {
                extractFieldMethodMap(processingEnv, paramClass).forEach((f, m) -> {
                    var objectElement = new ObjectField(p.getSimpleName().toString(), f);
                    extractedMap.put(objectElement, m);
                });
            }
        });
        var procedureName = CaseConverter.toSnakeCase(methodName).toUpperCase();
        var statement = new ProcedureExecutionStatement(procedureName);
        extractedMap.forEach((f, m) -> {
            statement.addParameter(CaseConverter.toSnakeCase(f.getField().getSimpleName().toString()));
        });
        var setStatements = generateSetStatements(processingEnv, extractedMap);

        ST templateBuilder = new ST(Templates.METHOD_TEMPLATE);
        templateBuilder.add(TemplateParams.STATEMENT_STATIC_CALL.getValue(), statement.build());
        templateBuilder.add(TemplateParams.RETURN_TYPE.getValue(), returnType);
        templateBuilder.add(TemplateParams.METHOD_NAME.getValue(), methodName);
        templateBuilder.add(TemplateParams.PARAMETERS.getValue(), parameters);
        templateBuilder.add(TemplateParams.DATA_SOURCE.getValue(), procedure.dataSource());
        templateBuilder.add(TemplateParams.PROCEDURE_FULL_NAME.getValue(), procedureName);
        templateBuilder.add(TemplateParams.STATEMENT_POPULATION.getValue(), setStatements);
        return templateBuilder.render();
    }

    private String generateClass(TypeElement clazz, List<String> generatedMethods) {
        // Create the new class name
        String originalClassName = clazz.getSimpleName().toString();
        String generatedClassName = originalClassName + "Impl";
        String packageName = getPackageName(clazz);

        ST templateBuilder = new ST(Templates.CLASS_TEMPLATE);
        templateBuilder.add(TemplateParams.PACKAGE_NAME.getValue(), packageName);
        templateBuilder.add(TemplateParams.CLASS_NAME.getValue(), generatedClassName);
        templateBuilder.add(TemplateParams.EXTENDED_CLASS_NAME.getValue(), originalClassName);
        templateBuilder.add(TemplateParams.METHODS.getValue(), generatedMethods);
        return templateBuilder.render();
    }


}
