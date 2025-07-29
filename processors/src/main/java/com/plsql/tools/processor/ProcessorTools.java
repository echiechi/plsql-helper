package com.plsql.tools.processor;

import com.plsql.tools.enums.JavaType;
import com.plsql.tools.processor.mapping.ObjectField;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcessorTools {
    public static String getPackageName(TypeElement clazz) {
        Element enclosingElement = clazz.getEnclosingElement();
        while (enclosingElement != null && enclosingElement.getKind() != ElementKind.PACKAGE) {
            enclosingElement = enclosingElement.getEnclosingElement();
        }
        if (enclosingElement != null) {
            return ((PackageElement) enclosingElement).getQualifiedName().toString();
        }
        return "";
    }

    public static TypeElement findElementByName(RoundEnvironment roundEnv, String className) {
        for (Element rootElement : roundEnv.getRootElements()) {
            if (rootElement instanceof TypeElement typeElement) {
                if (typeElement.getQualifiedName().toString().equals(className)) {
                    return typeElement;
                }
            }
        }
        return null;
    }

    public static void kindMessage(ProcessingEnvironment processingEnv, String message) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE, message
        );
    }

    public static void errorMessage(ProcessingEnvironment processingEnv, String message) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR, message
        );
    }

    public static Map<Element, Element> extractFieldMethodMap(ProcessingEnvironment processingEnv, TypeElement paramClass) {
        Map<Element, Element> fieldMethodMap = new LinkedHashMap<>();
        for (Element member : paramClass.getEnclosedElements()) {
            if (member.getKind() == ElementKind.FIELD) {
                kindMessage(processingEnv, member.toString());
                var method = paramClass
                        .getEnclosedElements()
                        .stream()
                        .filter(m -> m.getKind() == ElementKind.METHOD)
                        .filter(m -> isGetter(extractNameAsStr(m)))// TODO: something more robust than lowercase ?
                        .filter(m -> extractNameAsStr(m).toLowerCase().endsWith(extractNameAsStr(member).toLowerCase()))
                        .findAny();
                if (method.isPresent()) {
                    fieldMethodMap.put(member, method.get());
                } else if (isModifierPresent(member, Modifier.PUBLIC)) {
                    fieldMethodMap.put(member, null);
                }
            }
        }
        return fieldMethodMap;
    }

    // TODO : handle null values
    public static List<String> generateSetStatements(ProcessingEnvironment processingEnv, Map<ObjectField, Element> fieldMethodMap) {
        List<String> list = new ArrayList<>();
        fieldMethodMap.forEach((f, m) -> {
            var type = JavaType.fromSimpleName(f.getField().asType().toString());
            if (type != null) {
                var setter = type.getJdbcSetterMethod();
                var getter = m != null ? String.format("%s.%s()", f.getObjectName(), m.getSimpleName().toString()) :
                        String.format("%s.%s", f.getObjectName(), f.getField().getSimpleName().toString());
                if (type.isDateTime()) {
                    list.add(String.format("stmt.%s(pos++, %s);", setter, handleDate(getter, type)));
                } else if( type == JavaType.CHARACTER){
                    list.add(String.format("stmt.%s(pos++, String.valueOf(%s));", setter, getter));
                }else{
                    list.add(String.format("stmt.%s(pos++, %s);", setter, getter));
                }
            } else {
                errorMessage(processingEnv, "Unmapped java type " + f.getField().asType());
            }
        });
        return list;
    }

    private static String handleDate(String dateObjName, JavaType type) {
        return switch (type) {
            case DATE ->
                    String.format("java.sql.Date.valueOf(%s.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())", dateObjName);
            case LOCAL_DATE -> String.format("java.sql.Date.valueOf(%s)", dateObjName);
            case LOCAL_DATE_TIME -> String.format("java.sql.Timestamp.valueOf(%s)", dateObjName);
            case LOCAL_TIME -> String.format("java.sql.Time.valueOf(%s)", dateObjName);
            default -> "";
        };
    }

    public static boolean isGetter(String name) {
        return name != null && (name.startsWith("get") || name.startsWith("is"));
    }

    public static boolean isModifierPresent(Element element, Modifier modifier) {
        return element.getModifiers().contains(modifier);
    }

    public static String extractNameAsStr(Element name) {
        if (name == null || name.getSimpleName() == null) {
            return "";
        }
        return name.getSimpleName().toString();
    }
}
