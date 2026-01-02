package com.plsql.tools;

import com.plsql.tools.annotations.Package;
import com.plsql.tools.annotations.Record;
import com.plsql.tools.processors.RecordProcessor;
import com.plsql.tools.tools.Tools;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Improved version of PLSQLAnnotationProcessor with better error handling,
 * separation of concerns, and enhanced validation.
 */
@SupportedAnnotationTypes({
        "com.plsql.tools.annotations.Package",
        "com.plsql.tools.annotations.PlsqlCallable",
        "com.plsql.tools.annotations.Function",
        "com.plsql.tools.annotations.Record"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class PLSQLAnnotationProcessor extends AbstractProcessor {
    // TODO : create a cache also of templates => new Obj() obj.setName(rs.getString("colName")); ....
    // TODO : Potential recursion problem fix no nested classes of the same type
    // TODO : handle primitive type as return : throw exception and error message ?
    // TODO : handle STREAM from resultSet
    // TODO : look into n+1 problem
    private Filer filer;
    private ProcessingContext context;
    private RecordProcessor recordProcessor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.context = new ProcessingContext(processingEnv, Boolean.parseBoolean(System.getProperty("debug")));
        this.recordProcessor = new RecordProcessor(context);
        context.logInfoDeco("PL/SQL Annotation Processor initialized");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        context.logInfo("PL/SQL Started Processing");

        try {
            context.logInfo("Start processing classes annotated with Record.class");
            var recordElements = roundEnv.getElementsAnnotatedWith(Record.class);
            context.logInfo("**Classes Found**", recordElements.stream().map(Element::getSimpleName).collect(Collectors.joining(" | ")));
            for (var record : recordElements) {
                this.recordProcessor.process(record);
            }
            return processPackageClasses(roundEnv);
        } catch (Exception e) {
            context.logError("Fatal error during processing: " + e.getMessage());
            return false;
        }
    }

    private boolean processPackageClasses(RoundEnvironment roundEnv) {
        Set<? extends Element> packageClasses = roundEnv.getElementsAnnotatedWith(Package.class);

        context.logInfoDeco("Generating Implementation for Package annotated abstract classes");
        if (packageClasses.isEmpty()) {
            context.logInfo("No @Package annotated classes found");
            return true;
        }

        for (Element packageClass : packageClasses) {
            if (!processPackageClass(packageClass, roundEnv)) {
                return false;
            }
        }

        return true;
    }

    private boolean processPackageClass(Element packageClass, RoundEnvironment roundEnv) {
        if (!validatePackageClass(packageClass)) {
            return true; // Continue processing other classes
        }

        try {
            EnclosingClassProcessor processor = new EnclosingClassProcessor(context, (TypeElement) packageClass);

            String generatedClass = processor.generateImplementation();

            if (generatedClass != null) {
                writeGeneratedClass(packageClass, generatedClass);
            }

            return true;
        } catch (Exception e) {
            context.logError("Error processing package class " + packageClass.getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean validatePackageClass(Element packageClass) {
        if (packageClass.getKind() != ElementKind.CLASS) {
            context.logError("@Package can only be applied to classes: " + packageClass.getSimpleName());
            return false;
        }

        if (!(packageClass instanceof TypeElement)) {
            context.logError("Invalid package class type: " + packageClass.getSimpleName());
            return false;
        }

        return true;
    }

    private void writeGeneratedClass(Element packageClass, String generatedClass) throws IOException {
        String originalClassName = packageClass.getSimpleName().toString();
        String generatedClassName = originalClassName + "Impl";
        String packageName = Tools.getPackageNameSafe((TypeElement) packageClass).orElse("");

        JavaFileObject builderFile = filer.createSourceFile(packageName + "." + generatedClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.print(generatedClass);
        }

        context.logInfo("Generated implementation class: " + generatedClassName);
    }
}
