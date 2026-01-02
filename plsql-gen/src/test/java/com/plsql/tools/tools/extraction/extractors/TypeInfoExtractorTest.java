package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TypeInfoExtractor Tests")
class TypeInfoExtractorTest {

    @Mock
    private ProcessingContext context;

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Types typeUtils;

    @Mock
    private Elements elementsUtils;

    @Mock
    private Element element;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private DeclaredType declaredType;

    @Mock
    private TypeElement collectionTypeElement;

    @Mock
    private TypeElement optionalTypeElement;

    private TypeInfoExtractor extractor;

    @BeforeEach
    void setUp() {
        when(context.getProcessingEnv()).thenReturn(processingEnv);
        when(processingEnv.getTypeUtils()).thenReturn(typeUtils);
        when(processingEnv.getElementUtils()).thenReturn(elementsUtils);

        extractor = new TypeInfoExtractor(context);
    }

    @Test
    @DisplayName("extractTypeInfo should extract basic type info from Element")
    void testExtractTypeInfo_basicElement() {
        // Arrange
        when(element.asType()).thenReturn(typeMirror);
        when(typeMirror.getKind()).thenReturn(TypeKind.INT);

        // Act
        TypeInfo result = extractor.extractTypeInfo(element);

        // Assert
        assertNotNull(result);
        assertEquals(typeMirror, result.getMirror());
        assertEquals(element, result.getRawType());
        assertNull(result.getWrappedType());
        assertNull(result.getRawWrappedType());
    }

    @Test
    @DisplayName("extractTypeInfo should handle declared type with wrapped type")
    void testExtractTypeInfo_declaredTypeWithWrappedType() {
        // Arrange
        TypeMirror wrappedTypeMirror = mock(TypeMirror.class);
        Element wrappedElement = mock(Element.class);

        when(element.asType()).thenReturn(declaredType);
        when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        when(declaredType.getTypeArguments()).thenAnswer(inv -> List.of(wrappedTypeMirror));
        when(typeUtils.asElement(wrappedTypeMirror)).thenReturn(wrappedElement);

        // Act
        TypeInfo result = extractor.extractTypeInfo(element);

        // Assert
        assertNotNull(result);
        assertEquals(declaredType, result.getMirror());
        assertEquals(element, result.getRawType());
        assertEquals(wrappedTypeMirror, result.getWrappedType());
        assertEquals(wrappedElement, result.getRawWrappedType());
    }

    @Test
    @DisplayName("extractTypeInfo should handle declared type without type arguments")
    void testExtractTypeInfo_declaredTypeWithoutTypeArguments() {
        // Arrange
        when(element.asType()).thenReturn(declaredType);
        when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        when(declaredType.getTypeArguments()).thenReturn(Collections.emptyList());

        // Act
        TypeInfo result = extractor.extractTypeInfo(element);

        // Assert
        assertNotNull(result);
        assertEquals(declaredType, result.getMirror());
        assertEquals(element, result.getRawType());
        assertNull(result.getWrappedType());
        assertNull(result.getRawWrappedType());
    }

    @Test
    @DisplayName("extractTypeInfo from DeclaredType should extract type info correctly")
    void testExtractTypeInfo_fromDeclaredType() {
        // Arrange
        Element declaredElement = mock(Element.class);
        when(declaredType.asElement()).thenReturn(declaredElement);
        when(declaredType.getTypeArguments()).thenReturn(Collections.emptyList());

        // Act
        TypeInfo result = extractor.extractTypeInfo(declaredType);

        // Assert
        assertNotNull(result);
        assertEquals(declaredType, result.getMirror());
        assertEquals(declaredElement, result.getRawType());
        assertNull(result.getWrappedType());
    }

    @Test
    @DisplayName("extractTypeInfo from DeclaredType should handle wrapped types")
    void testExtractTypeInfo_fromDeclaredTypeWithWrappedType() {
        // Arrange
        Element declaredElement = mock(Element.class);
        TypeMirror wrappedTypeMirror = mock(TypeMirror.class);
        Element wrappedElement = mock(Element.class);

        when(declaredType.asElement()).thenReturn(declaredElement);
        when(declaredType.getTypeArguments()).thenAnswer(inv -> List.of(wrappedTypeMirror));
        when(typeUtils.asElement(wrappedTypeMirror)).thenReturn(wrappedElement);

        // Act
        TypeInfo result = extractor.extractTypeInfo(declaredType);

        // Assert
        assertNotNull(result);
        assertEquals(declaredType, result.getMirror());
        assertEquals(declaredElement, result.getRawType());
        assertEquals(wrappedTypeMirror, result.getWrappedType());
        assertEquals(wrappedElement, result.getRawWrappedType());
    }

    @Test
    @DisplayName("isCollection should return true for Collection types")
    void testIsCollection_withCollectionType() {
        // Arrange
        TypeMirror collectionType = mock(TypeMirror.class);
        TypeMirror erasedType = mock(TypeMirror.class);
        TypeMirror baseErasedType = mock(TypeMirror.class);

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
    @DisplayName("isCollection should return false for non-Collection types")
    void testIsCollection_withNonCollectionType() {
        // Arrange
        TypeMirror collectionType = mock(TypeMirror.class);
        TypeMirror erasedType = mock(TypeMirror.class);
        TypeMirror baseErasedType = mock(TypeMirror.class);

        when(elementsUtils.getTypeElement("java.util.Collection")).thenReturn(collectionTypeElement);
        when(collectionTypeElement.asType()).thenReturn(collectionType);
        when(typeUtils.erasure(typeMirror)).thenReturn(erasedType);
        when(typeUtils.erasure(collectionType)).thenReturn(baseErasedType);
        when(typeUtils.isAssignable(erasedType, baseErasedType)).thenReturn(false);

        // Act
        boolean result = extractor.isCollection(typeMirror);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("isCollection should return false when TypeElement is null")
    void testIsCollection_withNullTypeElement() {
        // Arrange
        when(elementsUtils.getTypeElement("java.util.Collection")).thenReturn(null);

        // Act
        boolean result = extractor.isCollection(typeMirror);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("isOptional should return true for Optional types")
    void testIsOptional_withOptionalType() {
        // Arrange
        TypeMirror optionalType = mock(TypeMirror.class);
        TypeMirror erasedType = mock(TypeMirror.class);
        TypeMirror baseErasedType = mock(TypeMirror.class);

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
    @DisplayName("isOptional should return false for non-Optional types")
    void testIsOptional_withNonOptionalType() {
        // Arrange
        TypeMirror optionalType = mock(TypeMirror.class);
        TypeMirror erasedType = mock(TypeMirror.class);
        TypeMirror baseErasedType = mock(TypeMirror.class);

        when(elementsUtils.getTypeElement("java.util.Optional")).thenReturn(optionalTypeElement);
        when(optionalTypeElement.asType()).thenReturn(optionalType);
        when(typeUtils.erasure(typeMirror)).thenReturn(erasedType);
        when(typeUtils.erasure(optionalType)).thenReturn(baseErasedType);
        when(typeUtils.isAssignable(erasedType, baseErasedType)).thenReturn(false);

        // Act
        boolean result = extractor.isOptional(typeMirror);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("isOptional should return false when TypeElement is null")
    void testIsOptional_withNullTypeElement() {
        // Arrange
        when(elementsUtils.getTypeElement("java.util.Optional")).thenReturn(null);

        // Act
        boolean result = extractor.isOptional(typeMirror);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("extractTypeInfo should handle multiple type arguments and extract only first")
    void testExtractTypeInfo_multipleTypeArguments() {
        // Arrange
        TypeMirror firstTypeArg = mock(TypeMirror.class);
        TypeMirror secondTypeArg = mock(TypeMirror.class);
        Element firstElement = mock(Element.class);

        when(element.asType()).thenReturn(declaredType);
        when(declaredType.getKind()).thenReturn(TypeKind.DECLARED);
        when(declaredType.getTypeArguments()).thenAnswer(inv -> List.of(firstTypeArg, secondTypeArg));
        when(typeUtils.asElement(firstTypeArg)).thenReturn(firstElement);

        // Act
        TypeInfo result = extractor.extractTypeInfo(element);

        // Assert
        assertNotNull(result);
        assertEquals(firstTypeArg, result.getWrappedType());
        assertEquals(firstElement, result.getRawWrappedType());
        verify(typeUtils, times(1)).asElement(firstTypeArg);
        verify(typeUtils, never()).asElement(secondTypeArg);
    }

    @Test
    @DisplayName("extractTypeInfo should handle primitive types")
    void testExtractTypeInfo_primitiveType() {
        // Arrange
        when(element.asType()).thenReturn(typeMirror);
        when(typeMirror.getKind()).thenReturn(TypeKind.INT);

        // Act
        TypeInfo result = extractor.extractTypeInfo(element);

        // Assert
        assertNotNull(result);
        assertEquals(typeMirror, result.getMirror());
        assertEquals(element, result.getRawType());
        assertFalse(result.isWrapped());
    }

    @Test
    @DisplayName("extractTypeInfo should handle void type")
    void testExtractTypeInfo_voidType() {
        // Arrange
        when(element.asType()).thenReturn(typeMirror);
        when(typeMirror.getKind()).thenReturn(TypeKind.VOID);

        // Act
        TypeInfo result = extractor.extractTypeInfo(element);

        // Assert
        assertNotNull(result);
        assertEquals(typeMirror, result.getMirror());
        assertEquals(element, result.getRawType());
        assertFalse(result.isWrapped());
    }
}
