package com.plsql.tools.tools.extraction;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Extractor Facade Tests")
class ExtractorTest {

    @Mock
    private ProcessingContext context;

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Types typeUtils;

    @Mock
    private Elements elementsUtils;

    @Mock
    private SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    @Mock
    private Element element;

    @Mock
    private ExecutableElement method;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private DeclaredType declaredType;

    @Mock
    private Name elementName;

    private Extractor extractor;

    @BeforeEach
    void setUp() {
        when(context.getProcessingEnv()).thenReturn(processingEnv);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);
        when(processingEnv.getElementUtils()).thenReturn(elementsUtils);
        when(context.getCache()).thenReturn(cache);

        extractor = new Extractor(context);
    }

    @Test
    @DisplayName("constructor should initialize with valid context")
    void testConstructor_validContext() {
        // Act & Assert
        assertNotNull(extractor);
        assertNotNull(extractor.getContext());
        assertEquals(context, extractor.getContext());
    }

    @Test
    @DisplayName("context should throw exception when context is null")
    void testContext_nullContext() {
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new Extractor(null)
        );

        assertEquals("Context is not yet defined", exception.getMessage());
    }

    @Test
    @DisplayName("getAttachedElements should return elements from cache")
    void testGetAttachedElements_fromCache() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        List<AttachedElementInfo> expectedElements = List.of(attachedElement);

        when(cache.get(typeMirror)).thenReturn(Optional.of(expectedElements));

        // Act
        List<AttachedElementInfo> result = extractor.getAttachedElements(typeMirror);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedElements, result);
        verify(cache).get(typeMirror);
    }

    @Test
    @DisplayName("getAttachedElements should return empty list when cache is empty")
    void testGetAttachedElements_emptyCach() {
        // Arrange
        when(cache.get(typeMirror)).thenReturn(Optional.empty());

        // Act
        List<AttachedElementInfo> result = extractor.getAttachedElements(typeMirror);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cache).get(typeMirror);
    }

    @Test
    @DisplayName("isCollection should delegate to TypeInfoExtractor")
    void testIsCollection_delegatesToTypeInfoExtractor() {
        // Arrange
        TypeMirror collectionType = mock(TypeMirror.class);
        TypeMirror erasedType = mock(TypeMirror.class);
        TypeMirror baseErasedType = mock(TypeMirror.class);
        var collectionTypeElement = mock(javax.lang.model.element.TypeElement.class);

        when(elementsUtils.getTypeElement("java.util.Collection")).thenReturn(collectionTypeElement);
        when(collectionTypeElement.asType()).thenReturn(collectionType);
        when(typeUtils.erasure(typeMirror)).thenReturn(erasedType);
        when(typeUtils.erasure(collectionType)).thenReturn(baseErasedType);
        when(typeUtils.isAssignable(erasedType, baseErasedType)).thenReturn(true);

        // Act
        boolean result = extractor.isCollection(typeMirror);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("isOptional should delegate to TypeInfoExtractor")
    void testIsOptional_delegatesToTypeInfoExtractor() {
        // Arrange
        TypeMirror optionalType = mock(TypeMirror.class);
        TypeMirror erasedType = mock(TypeMirror.class);
        TypeMirror baseErasedType = mock(TypeMirror.class);
        var optionalTypeElement = mock(javax.lang.model.element.TypeElement.class);

        when(elementsUtils.getTypeElement("java.util.Optional")).thenReturn(optionalTypeElement);
        when(optionalTypeElement.asType()).thenReturn(optionalType);
        when(typeUtils.erasure(typeMirror)).thenReturn(erasedType);
        when(typeUtils.erasure(optionalType)).thenReturn(baseErasedType);
        when(typeUtils.isAssignable(erasedType, baseErasedType)).thenReturn(true);

        // Act
        boolean result = extractor.isOptional(typeMirror);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("extractClassInfoAndAlimCache should extract and cache class info")
    void testExtractClassInfoAndAlimCache_newClass() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        List<AttachedElementInfo> expectedElements = List.of(attachedElement);

        when(element.asType()).thenReturn(declaredType);
        when(element.getKind()).thenReturn(javax.lang.model.element.ElementKind.CLASS);
//        when(element.getSimpleName()).thenReturn(elementName);
//        when(elementName.toString()).thenReturn("TestClass");

        when(cache.get(declaredType)).thenReturn(Optional.empty());

        var constructor = mock(ExecutableElement.class);
        when(element.getEnclosedElements()).thenAnswer(inv -> List.of(constructor));
        when(constructor.getKind()).thenReturn(javax.lang.model.element.ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        // Act
        extractor.extractClassInfoAndAlimCache(element);

        // Assert
        verify(cache).get(declaredType);
        verify(cache).put(eq(declaredType), any());
    }

    @Test
    @DisplayName("extractClassInfoAndAlimCache should return cached data when available")
    void testExtractClassInfoAndAlimCache_cachedClass() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        List<AttachedElementInfo> cachedElements = List.of(attachedElement);

        when(element.asType()).thenReturn(declaredType);
        when(cache.get(declaredType)).thenReturn(Optional.of(cachedElements));

        // Act
        extractor.extractClassInfoAndAlimCache(element);

        // Assert
        verify(cache).get(declaredType);
        verify(cache, never()).put(any(), any());
    }

    @Test
    @DisplayName("extractReturn should delegate to ReturnExtractor")
    void testExtractReturn_delegatesToReturnExtractor() {
        // Arrange
        TypeMirror voidType = mock(TypeMirror.class);
        when(method.getReturnType()).thenReturn(voidType);
        when(voidType.toString()).thenReturn("void");

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractPramNames should delegate to ParameterExtractor")
    void testExtractPramNames_delegatesToParameterExtractor() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);
        ElementInfo element1 = new ElementInfo(typeInfo, "param1");
        ElementInfo element2 = new ElementInfo(typeInfo, "param2");

        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.getRawType()).thenReturn(mock(Element.class));
        // Act
        List<String> result = extractor.extractPramNames(List.of(element1, element2));

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("param1", result.get(0));
        assertEquals("param2", result.get(1));
    }

    @Test
    @DisplayName("extractParams should delegate to ParameterExtractor")
    void testExtractParams_delegatesToParameterExtractor() {
        // Arrange
        when(method.getParameters()).thenReturn(Collections.emptyList());

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    @Test
    @DisplayName("extractClassInfoAndAlimCache should handle record types")
    void testExtractClassInfoAndAlimCache_recordType() {
        // Arrange
        when(element.asType()).thenReturn(declaredType);
        when(element.getKind()).thenReturn(javax.lang.model.element.ElementKind.RECORD);
//        when(element.getSimpleName()).thenReturn(elementName);
//        when(elementName.toString()).thenReturn("TestRecord");

        when(cache.get(declaredType)).thenReturn(Optional.empty());

        when(element.getEnclosedElements()).thenReturn(Collections.emptyList());

        // Act
        extractor.extractClassInfoAndAlimCache(element);

        // Assert
        verify(cache).get(declaredType);
        verify(cache).put(eq(declaredType), any());
    }

    @Test
    @DisplayName("extractParams should handle methods with parameters")
    void testExtractParams_withParameters() {
        // Arrange
        var parameter = mock(javax.lang.model.element.VariableElement.class);
        Name paramName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(method.getParameters()).thenAnswer(inv -> List.of(parameter));
        when(parameter.getSimpleName()).thenReturn(paramName);
        when(paramName.toString()).thenReturn("userId");
        when(parameter.asType()).thenReturn(typeMirror);
        when(typeMirror.getKind()).thenReturn(TypeKind.INT);

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("userId", result.get(0).getName());
    }

    @Test
    @DisplayName("isCollection should return false for non-collection types")
    void testIsCollection_nonCollectionType() {
        // Arrange
        when(elementsUtils.getTypeElement("java.util.Collection")).thenReturn(null);

        // Act
        boolean result = extractor.isCollection(typeMirror);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("isOptional should return false for non-optional types")
    void testIsOptional_nonOptionalType() {
        // Arrange
        when(elementsUtils.getTypeElement("java.util.Optional")).thenReturn(null);

        // Act
        boolean result = extractor.isOptional(typeMirror);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("extractPramNames should handle empty list")
    void testExtractPramNames_emptyList() {
        // Act
        List<String> result = extractor.extractPramNames(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("convertInto should handle complex nested structures")
    void testConvertInto_nestedStructures() {
        // Arrange
        Element record = mock(Element.class);
        Name recordName = mock(Name.class);
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        TypeInfo attachedTypeInfo = mock(TypeInfo.class);

        when(record.asType()).thenReturn(typeMirror);
        when(record.getSimpleName()).thenReturn(recordName);
        when(recordName.toString()).thenReturn("ComplexRecord");

        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(attachedElement)));

        when(attachedElement.getTypeInfo()).thenReturn(attachedTypeInfo);
        when(attachedTypeInfo.isSimple()).thenReturn(true);

        // Act
        ComposedElementInfo result = extractor.convertInto(record);

        // Assert
        assertNotNull(result);
        assertEquals("ComplexRecord", result.getName());
        assertEquals(1, result.getElementInfoList().size());
    }
}
