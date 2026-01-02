package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.Tools;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.plsql.tools.tools.Tools.extractNameAsStr;
import static com.plsql.tools.tools.Tools.findMatchingMethod;
import static com.plsql.tools.tools.ValidationUtils.isValidGetter;
import static com.plsql.tools.tools.ValidationUtils.isValidSetter;
import static com.plsql.tools.utils.CaseConverter.upperCaseFirstLetter;

public class ClassInfoExtractor {

    private final TypeInfoExtractor typeInfoExtractor;
    private final ProcessingContext context;

    public ClassInfoExtractor(TypeInfoExtractor typeInfoExtractor, ProcessingContext context) {
        this.typeInfoExtractor = typeInfoExtractor;
        this.context = context;
    }

    public List<AttachedElementInfo> extractClassInfo(Element classOrRecord) {
        validateClassOrRecord(classOrRecord);
        ClassMetadata metaData = extractClassMetadata(classOrRecord);

        if (metaData.kind() == ElementKind.CLASS) {
            var constructor = metaData.constructors().stream()
                    .filter(c -> c.getParameters().isEmpty())
                    .findFirst();
            if (constructor.isEmpty()) {
                throw new IllegalStateException("No default constructor is present");
            }
        }

        return createAttachedElements(classOrRecord, metaData);
    }

    private void validateClassOrRecord(Element element) {
        if (element == null) {
            throw new IllegalArgumentException("Class or Record element cannot be null");
        }
        ElementKind kind = element.getKind();
        if (kind != ElementKind.CLASS && kind != ElementKind.RECORD) {
            throw new IllegalArgumentException(
                    "Element must be a CLASS or RECORD, but was: " + kind
            );
        }
    }

    private List<AttachedElementInfo> createAttachedElements(
            Element classOrRecord,
            ClassMetadata metadata
    ) {
        return metadata.fields().stream()
                .map(field -> createAttachedElement(field, classOrRecord.getKind(), metadata))
                .collect(Collectors.toList());
    }

    private AttachedElementInfo createAttachedElement(Element field, ElementKind kind, ClassMetadata metaData) {
        var attachedElement = new AttachedElementInfo();
        attachedElement.setName(extractNameAsStr(field));
        attachedElement.setTypeInfo(typeInfoExtractor.extractTypeInfo(field));

        if (kind == ElementKind.RECORD) {
            populateRecordAccessors(attachedElement, metaData.methods());
        } else if (kind == ElementKind.CLASS) {
            attachedElement.setPublic(Tools.isModifierPresent(field, Modifier.PUBLIC));
            populateClassAccessors(attachedElement, metaData.methods());
        }
        return attachedElement;
    }


    private ClassMetadata extractClassMetadata(Element classOrRecord) {
        List<Element> fields = extractFields(classOrRecord);
        List<ExecutableElement> methods = extractMethods(classOrRecord);
        List<ExecutableElement> constructors = extractConstructors(classOrRecord);

        return new ClassMetadata(
                classOrRecord.getKind(),
                fields,
                methods,
                constructors
        );
    }

    private void populateRecordAccessors(AttachedElementInfo attachedElement, List<ExecutableElement> methods) {
        for (var method : methods) {
            if (extractNameAsStr(method).equals(attachedElement.getName())) {
                attachedElement.setGetter(method);// should always be present
                break;
            }
        }
    }

    private void populateClassAccessors(AttachedElementInfo attachedElement,
                                        List<ExecutableElement> methods
    ) {
        Optional<ExecutableElement> matchingGetter = findMatchingMethod(attachedElement.getName(),
                methods,
                method -> isValidGetter(extractNameAsStr(method)) &&
                        extractNameAsStr(method).endsWith(upperCaseFirstLetter(attachedElement.getName()))
        );
        Optional<ExecutableElement> matchingSetter = findMatchingMethod(attachedElement.getName(),
                methods,
                method -> isValidSetter(extractNameAsStr(method)) &&
                        extractNameAsStr(method).endsWith(upperCaseFirstLetter(attachedElement.getName()))
        );
        if (matchingGetter.isPresent()) {
            attachedElement.setGetter(matchingGetter.get());
        } else if (attachedElement.isPublic()) {
            context.logWarning("Using direct field access for: " + attachedElement.getName() + " (no getter found)");
        } else {
            context.logWarning("Skipping private field without getter: " + attachedElement.getName());
        }
        matchingSetter.ifPresent(attachedElement::setSetter);
    }


    private List<Element> extractFields(Element record) {
        return record.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .collect(Collectors.toList());
    }

    private List<ExecutableElement> extractMethods(Element paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .collect(Collectors.toList());
    }

    public List<ExecutableElement> extractConstructors(Element paramClass) {
        return paramClass.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .collect(Collectors.toList());
    }

    private record ClassMetadata(
            ElementKind kind,
            List<Element> fields,
            List<ExecutableElement> methods,
            List<ExecutableElement> constructors
    ) {
    }
}
