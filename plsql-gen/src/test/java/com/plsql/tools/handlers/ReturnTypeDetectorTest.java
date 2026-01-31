package com.plsql.tools.handlers;

import com.plsql.tools.handlers.ReturnTypeDetector.ReturnCategory;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.type.TypeMirror;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReturnTypeDetector.
 * Tests the categorization logic for different return types.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReturnTypeDetector Tests")
class ReturnTypeDetectorTest {

    @Mock
    private Extractor extractor;

    @Mock
    private ReturnElementInfo returnElementInfo;

    @Mock
    private TypeInfo typeInfo;

    @Mock
    private TypeMirror typeMirror;

    private ReturnTypeDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ReturnTypeDetector(extractor);
    }

    @Test
    @DisplayName("constructor should initialize with valid extractor")
    void constructor_validExtractor_shouldInitialize() {
        // Act
        ReturnTypeDetector newDetector = new ReturnTypeDetector(extractor);

        // Assert
        assertNotNull(newDetector);
    }

    @Test
    @DisplayName("categorize should return SIMPLE for simple types")
    void categorize_simpleType_shouldReturnSimple() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.SIMPLE, result);
        verify(typeInfo).isSimple();
    }

    @Test
    @DisplayName("categorize should return COMPOSED for non-wrapped complex types")
    void categorize_composedType_shouldReturnComposed() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(false);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.COMPOSED, result);
        verify(typeInfo).isSimple();
        verify(typeInfo).isWrapped();
    }

    @Test
    @DisplayName("categorize should return OPTIONAL_SIMPLE for Optional<SimpleType>")
    void categorize_optionalSimple_shouldReturnOptionalSimple() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.OPTIONAL_SIMPLE, result);
        verify(typeInfo).isSimple();
        verify(typeInfo, atLeastOnce()).isWrapped();
        verify(typeInfo, atLeastOnce()).isWrappedSimple();
        verify(extractor, atLeastOnce()).isOptional(typeMirror);
    }

    @Test
    @DisplayName("categorize should return OPTIONAL_COMPOSED for Optional<ComposedType>")
    void categorize_optionalComposed_shouldReturnOptionalComposed() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.OPTIONAL_COMPOSED, result);
        verify(typeInfo, atLeastOnce()).isWrapped();
        verify(typeInfo, atLeastOnce()).isWrappedSimple();
        verify(extractor, atLeastOnce()).isOptional(typeMirror);
    }

    @Test
    @DisplayName("categorize should return COLLECTION for Collection types")
    void categorize_collectionType_shouldReturnCollection() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(false);
        when(extractor.isCollection(typeMirror)).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.COLLECTION, result);
        verify(extractor).isCollection(typeMirror);
    }

    @Test
    @DisplayName("categorize should throw IllegalStateException for unknown type")
    void categorize_unknownType_shouldThrowException() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(false);
        when(extractor.isCollection(typeMirror)).thenReturn(false);
        when(typeInfo.toString()).thenReturn("UnknownType");

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> detector.categorize(returnElementInfo)
        );

        assertTrue(exception.getMessage().contains("Unknown return type"));
    }

    @Test
    @DisplayName("categorize should handle wrapped simple with Optional correctly")
    void categorize_wrappedSimpleOptional_shouldCheckAllConditions() {
        // Arrange - Testing that isWrappedSimple is checked before isOptional
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.OPTIONAL_SIMPLE, result);
        verify(typeInfo).isWrappedSimple();
        verify(extractor).isOptional(typeMirror);
    }

    @Test
    @DisplayName("categorize should handle wrapped simple but not Optional")
    void categorize_wrappedSimpleNotOptional_shouldCheckCollection() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(false);
        when(extractor.isCollection(typeMirror)).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        // Even though wrapped is simple, if it's not Optional, it should check for Collection
        assertEquals(ReturnCategory.COLLECTION, result);
    }

    @Test
    @DisplayName("categorize should handle wrapped composed with Optional")
    void categorize_wrappedComposedOptional_shouldReturnOptionalComposed() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.OPTIONAL_COMPOSED, result);
    }

    @Test
    @DisplayName("categorize should prioritize SIMPLE over all other categories")
    void categorize_simpleType_shouldNotCheckOtherConditions() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.SIMPLE, result);
        // Verify that other methods are not called when type is simple
        verify(typeInfo, never()).isWrapped();
        verify(extractor, never()).isOptional(any());
        verify(extractor, never()).isCollection(any());
    }

    @Test
    @DisplayName("categorize should prioritize COMPOSED over wrapped types")
    void categorize_notWrappedComposed_shouldReturnComposed() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(false);

        // Act
        ReturnCategory result = detector.categorize(returnElementInfo);

        // Assert
        assertEquals(ReturnCategory.COMPOSED, result);
        // Verify that wrapped-specific checks are not performed
        verify(extractor, never()).isOptional(any());
        verify(extractor, never()).isCollection(any());
    }

    @Test
    @DisplayName("categorize should handle null typeMirror in wrapped type checks")
    void categorize_wrappedWithNullMirror_shouldHandleGracefully() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(null);

        // Act & Assert
        // This should throw NullPointerException when trying to check isOptional with null
        assertThrows(IllegalStateException.class, () -> detector.categorize(returnElementInfo));
    }

    @Test
    @DisplayName("ReturnCategory enum should have all expected values")
    void returnCategory_shouldHaveAllExpectedValues() {
        // Assert - Verify all enum values exist
        ReturnCategory[] categories = ReturnCategory.values();
        assertEquals(5, categories.length);

        // Verify each category exists
        assertNotNull(ReturnCategory.valueOf("SIMPLE"));
        assertNotNull(ReturnCategory.valueOf("COMPOSED"));
        assertNotNull(ReturnCategory.valueOf("OPTIONAL_SIMPLE"));
        assertNotNull(ReturnCategory.valueOf("OPTIONAL_COMPOSED"));
        assertNotNull(ReturnCategory.valueOf("COLLECTION"));
    }
}
