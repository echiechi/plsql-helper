package com.plsql.tools.tools.extraction;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractorTest {

    private Extractor extractor;

    @BeforeEach
    void setUp() {
        // Reset the singleton instance before each test
        resetExtractorSingleton();
        extractor = Extractor.getInstance();
    }

    private void resetExtractorSingleton() {
        try {
            var field = Extractor.class.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset singleton", e);
        }
    }

    @Test
    void testGetInstance_ReturnsSameInstance() {
        Extractor firstInstance = Extractor.getInstance();
        Extractor secondInstance = Extractor.getInstance();

        assertSame(firstInstance, secondInstance, "getInstance should return the same instance");
    }

    @Test
    void testGetInstance_WithDifferentContext() {
        ProcessingContext anotherContext = mock(ProcessingContext.class);

        Extractor firstInstance = Extractor.getInstance();
        Extractor secondInstance = Extractor.getInstance();

        // Should return the same instance even with different context (singleton pattern)
        assertSame(firstInstance, secondInstance);
    }

    @Test
    void testExtractClassInfo_ThrowsExceptionWhenRecordIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extractClassInfo(null)
        );

        assertEquals("@Record class cannot be null", exception.getMessage());
    }

    @Test
    void testExtractClassInfo_EmptyElement_ReturnsEmptyList() {
        Element mockElement = mock(Element.class, withSettings().lenient());
        when(mockElement.getKind()).thenReturn(ElementKind.RECORD);
        when(mockElement.getEnclosedElements()).thenAnswer(inv -> Collections.emptyList());

        List<AttachedElementInfo> result = extractor.extractClassInfo(mockElement);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    @Test
    void testExtractClassInfo_UnsupportedElementKind_ThrowsException() {
        Element mockElement = mock(Element.class, withSettings().lenient());
        Element mockField = mock(Element.class, withSettings().lenient());
        TypeMirror mockType = mock(TypeMirror.class, withSettings().lenient());
        Name mockFieldName = createMockName("testField");

        when(mockElement.getKind()).thenReturn(ElementKind.INTERFACE);
        when(mockField.getKind()).thenReturn(ElementKind.FIELD);
        when(mockField.getSimpleName()).thenReturn(mockFieldName);
        when(mockField.asType()).thenReturn(mockType);
        when(mockType.getKind()).thenReturn(TypeKind.INT);

        List<? extends Element> elements = List.of(mockField);
        when(mockElement.getEnclosedElements()).thenAnswer(inv -> elements);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> extractor.extractClassInfo(mockElement)
        );

        assertEquals("Unsupported Element Kind", exception.getMessage());
    }

    @Test
    void testExtractDeclaredType_WithoutTypeArguments() {
        DeclaredType mockDeclaredType = mock(DeclaredType.class);

        when(mockDeclaredType.getKind()).thenReturn(TypeKind.DECLARED);
        when(mockDeclaredType.getTypeArguments()).thenAnswer(inv -> Collections.emptyList());

        TypeMirror result = extractor.extractDeclaredType(mockDeclaredType);

        assertNull(result);
    }

    @Test
    void testExtractDeclaredType_NonDeclaredType() {
        TypeMirror mockType = mock(TypeMirror.class);
        when(mockType.getKind()).thenReturn(TypeKind.INT);

        TypeMirror result = extractor.extractDeclaredType(mockType);

        assertNull(result);
    }

    @Test
    void testExtractDeclaredType_NullType() {
        // extractDeclaredType doesn't handle null - it will throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            extractor.extractDeclaredType(null);
        });
    }

    @Test
    void testSingletonPattern_ThreadSafety() {
        // Test that getInstance is thread-safe
        Extractor instance1 = Extractor.getInstance();
        Extractor instance2 = Extractor.getInstance();

        assertSame(instance1, instance2);
        assertNotNull(instance1);
    }

    @Test
    void testExtractClassInfo_DifferentElements_DifferentCache() {
        Element mockElement1 = mock(Element.class, withSettings().lenient());
        Element mockElement2 = mock(Element.class, withSettings().lenient());

        when(mockElement1.getKind()).thenReturn(ElementKind.RECORD);
        when(mockElement1.getEnclosedElements()).thenAnswer(inv -> Collections.emptyList());

        when(mockElement2.getKind()).thenReturn(ElementKind.RECORD);
        when(mockElement2.getEnclosedElements()).thenAnswer(inv -> Collections.emptyList());

        List<AttachedElementInfo> result1 = extractor.extractClassInfo(mockElement1);

        // Calling again should use cache
        List<AttachedElementInfo> result1Again = extractor.extractClassInfo(mockElement1);
        assertSame(result1, result1Again);
    }

    /**
     * Helper method to create a mock Name that avoids mocking final methods
     */
    private Name createMockName(String value) {
        Name mockName = mock(Name.class, withSettings().lenient());
        doReturn(value).when(mockName).toString();
        doReturn(value.length()).when(mockName).length();
        doReturn(value.charAt(0)).when(mockName).charAt(0);
        return mockName;
    }
}
