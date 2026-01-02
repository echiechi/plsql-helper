package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComposedElementExtractor Tests")
class ComposedElementExtractorTest {

    @Mock
    private TypeInfoExtractor typeInfoExtractor;

    @Mock
    private SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    @Mock
    private Element element;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private TypeInfo typeInfo;

    @Mock
    private Name elementName;

    private ComposedElementExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ComposedElementExtractor(typeInfoExtractor, cache);
    }

    @Test
    @DisplayName("convertInto should throw exception when element is null")
    void testConvertInto_nullElement() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> extractor.convertInto(null)
        );

        assertEquals("@Record class cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("convertInto should create ComposedElementInfo with basic element")
    void testConvertInto_basicElement() {
        // Arrange
        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);
        when(cache.contains(typeMirror)).thenReturn(false);

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertEquals("TestClass", result.getName());
        assertEquals(typeInfo, result.getTypeInfo());
        assertTrue(result.getElementInfoList().isEmpty());
        assertTrue(result.getNestedElementInfo().isEmpty());
    }

    @Test
    @DisplayName("convertInto should populate elements from cache")
    void testConvertInto_withCachedElements() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        TypeInfo attachedTypeInfo = mock(TypeInfo.class);

        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);
        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(attachedElement)));
        when(attachedElement.getTypeInfo()).thenReturn(attachedTypeInfo);
        when(attachedTypeInfo.isSimple()).thenReturn(true);
//        when(attachedTypeInfo.getMirror()).thenReturn(typeMirror);

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getElementInfoList().size());
        assertEquals(attachedElement, result.getElementInfoList().get(0));
    }

    @Test
    @DisplayName("convertInto should handle nested non-simple types")
    void testConvertInto_withNestedNonSimpleTypes() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        TypeInfo attachedTypeInfo = mock(TypeInfo.class);
        TypeMirror nestedTypeMirror = mock(TypeMirror.class);
        AttachedElementInfo nestedElement = mock(AttachedElementInfo.class);
        TypeInfo nestedTypeInfo = mock(TypeInfo.class);

        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);

        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(attachedElement)));

        when(attachedElement.getTypeInfo()).thenReturn(attachedTypeInfo);
        when(attachedTypeInfo.isSimple()).thenReturn(false);
        when(attachedTypeInfo.getMirror()).thenReturn(nestedTypeMirror);

        when(cache.get(nestedTypeMirror)).thenReturn(Optional.of(List.of(nestedElement)));
        when(nestedElement.getTypeInfo()).thenReturn(nestedTypeInfo);
        when(nestedTypeInfo.isSimple()).thenReturn(true);

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getElementInfoList().size());
        assertEquals(1, result.getNestedElementInfo().size());
        assertTrue(result.getNestedElementInfo().containsKey(nestedTypeMirror));
        verify(cache, times(2)).get(any(TypeMirror.class));
    }

    @Test
    @DisplayName("convertInto with TypeInfo should create ComposedElementInfo")
    void testConvertInto_withTypeInfo() {
        // Arrange
        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(cache.contains(typeMirror)).thenReturn(false);

        // Act
        ComposedElementInfo result = extractor.convertInto(element, typeInfo);

        // Assert
        assertNotNull(result);
        assertEquals("TestClass", result.getName());
        assertEquals(typeInfo, result.getTypeInfo());
        verify(typeInfoExtractor, never()).extractTypeInfo((Element) any());
    }

    @Test
    @DisplayName("convertInto should handle wrapped types in nested elements")
    void testConvertInto_withWrappedNestedTypes() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        TypeInfo attachedTypeInfo = mock(TypeInfo.class);
        TypeMirror outerTypeMirror = mock(TypeMirror.class);
        TypeMirror wrappedTypeMirror = mock(TypeMirror.class);
        AttachedElementInfo nestedElement = mock(AttachedElementInfo.class);
        TypeInfo nestedTypeInfo = mock(TypeInfo.class);

        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);

        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(attachedElement)));

        when(attachedElement.getTypeInfo()).thenReturn(attachedTypeInfo);
        when(attachedTypeInfo.isSimple()).thenReturn(false);
        when(attachedTypeInfo.getMirror()).thenReturn(outerTypeMirror);

        when(cache.get(outerTypeMirror)).thenReturn(Optional.of(List.of(nestedElement)));
        when(nestedElement.getTypeInfo()).thenReturn(nestedTypeInfo);
        when(nestedTypeInfo.isSimple()).thenReturn(false);
        when(nestedTypeInfo.isWrapped()).thenReturn(true);
        when(nestedTypeInfo.getWrappedType()).thenReturn(wrappedTypeMirror);

        when(cache.get(wrappedTypeMirror)).thenReturn(Optional.empty());

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        verify(cache).get(wrappedTypeMirror);
    }

    @Test
    @DisplayName("convertInto should handle empty cache")
    void testConvertInto_emptyCacheForNestedType() {
        // Arrange
        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        TypeInfo attachedTypeInfo = mock(TypeInfo.class);
        TypeMirror nestedTypeMirror = mock(TypeMirror.class);

        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);

        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(attachedElement)));

        when(attachedElement.getTypeInfo()).thenReturn(attachedTypeInfo);
        when(attachedTypeInfo.isSimple()).thenReturn(false);
        when(attachedTypeInfo.getMirror()).thenReturn(nestedTypeMirror);

        when(cache.get(nestedTypeMirror)).thenReturn(Optional.empty());

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getElementInfoList().size());
        assertEquals(0, result.getNestedElementInfo().size());
    }

    @Test
    @DisplayName("convertInto should handle multiple attached elements")
    void testConvertInto_multipleAttachedElements() {
        // Arrange
        AttachedElementInfo element1 = mock(AttachedElementInfo.class);
        AttachedElementInfo element2 = mock(AttachedElementInfo.class);
        TypeInfo typeInfo1 = mock(TypeInfo.class);
        TypeInfo typeInfo2 = mock(TypeInfo.class);

        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);

        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(element1, element2)));

        when(element1.getTypeInfo()).thenReturn(typeInfo1);
        when(element2.getTypeInfo()).thenReturn(typeInfo2);
        when(typeInfo1.isSimple()).thenReturn(true);
        when(typeInfo2.isSimple()).thenReturn(true);

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getElementInfoList().size());
        assertEquals(element1, result.getElementInfoList().get(0));
        assertEquals(element2, result.getElementInfoList().get(1));
    }

    @Test
    @DisplayName("convertInto should handle deeply nested objects")
    void testConvertInto_deeplyNestedObjects() {
        // Arrange
        AttachedElementInfo level1Element = mock(AttachedElementInfo.class);
        TypeInfo level1TypeInfo = mock(TypeInfo.class);
        TypeMirror level1TypeMirror = mock(TypeMirror.class);

        AttachedElementInfo level2Element = mock(AttachedElementInfo.class);
        TypeInfo level2TypeInfo = mock(TypeInfo.class);
        TypeMirror level2TypeMirror = mock(TypeMirror.class);

        AttachedElementInfo level3Element = mock(AttachedElementInfo.class);
        TypeInfo level3TypeInfo = mock(TypeInfo.class);

        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);

        // Level 1
        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(List.of(level1Element)));
        when(level1Element.getTypeInfo()).thenReturn(level1TypeInfo);
        when(level1TypeInfo.isSimple()).thenReturn(false);
        when(level1TypeInfo.getMirror()).thenReturn(level1TypeMirror);

        // Level 2
        when(cache.get(level1TypeMirror)).thenReturn(Optional.of(List.of(level2Element)));
        when(level2Element.getTypeInfo()).thenReturn(level2TypeInfo);
        when(level2TypeInfo.isSimple()).thenReturn(false);
        when(level2TypeInfo.isWrapped()).thenReturn(false);
        when(level2TypeInfo.getMirror()).thenReturn(level2TypeMirror);

        // Level 3
        when(cache.get(level2TypeMirror)).thenReturn(Optional.of(List.of(level3Element)));
        when(level3Element.getTypeInfo()).thenReturn(level3TypeInfo);
        when(level3TypeInfo.isSimple()).thenReturn(true);

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getElementInfoList().size());
        assertEquals(2, result.getNestedElementInfo().size());
        verify(cache, times(3)).get(any(TypeMirror.class));
    }

    @Test
    @DisplayName("convertInto should handle cache returning empty list")
    void testConvertInto_cacheReturnsEmptyList() {
        // Arrange
        when(element.asType()).thenReturn(typeMirror);
        when(element.getSimpleName()).thenReturn(elementName);
        when(elementName.toString()).thenReturn("TestClass");
        when(typeInfoExtractor.extractTypeInfo(element)).thenReturn(typeInfo);
        when(cache.contains(typeMirror)).thenReturn(true);
        when(cache.get(typeMirror)).thenReturn(Optional.of(Collections.emptyList()));

        // Act
        ComposedElementInfo result = extractor.convertInto(element);

        // Assert
        assertNotNull(result);
        assertTrue(result.getElementInfoList().isEmpty());
        assertTrue(result.getNestedElementInfo().isEmpty());
    }
}
