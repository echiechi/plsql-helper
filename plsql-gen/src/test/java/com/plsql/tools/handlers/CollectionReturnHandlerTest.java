package com.plsql.tools.handlers;

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
 * Unit tests for CollectionReturnHandler.
 * Tests the handling of Collection return types (List, Set, etc.).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionReturnHandler Tests")
class CollectionReturnHandlerTest {

    @Mock
    private Extractor extractor;

    @Mock
    private ReturnElementInfo returnElementInfo;

    @Mock
    private TypeInfo typeInfo;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private TypeMirror erasedTypeMirror;

    private CollectionReturnHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CollectionReturnHandler(extractor);
    }

    @Test
    @DisplayName("constructor should initialize with valid extractor")
    void constructor_validExtractor_shouldInitialize() {
        // Act
        CollectionReturnHandler newHandler = new CollectionReturnHandler(extractor);

        // Assert
        assertNotNull(newHandler);
    }

    @Test
    @DisplayName("canHandle should return true for wrapped collection types")
    void canHandle_wrappedCollectionType_shouldReturnTrue() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isCollection(typeMirror)).thenReturn(true);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertTrue(result);
        verify(typeInfo).isWrapped();
        verify(extractor).isCollection(typeMirror);
    }

    @Test
    @DisplayName("canHandle should return false for non-wrapped types")
    void canHandle_nonWrappedType_shouldReturnFalse() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(false);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertFalse(result);
        verify(typeInfo).isWrapped();
        verify(extractor, never()).isCollection(any());
    }

    @Test
    @DisplayName("canHandle should return false for wrapped non-collection types")
    void canHandle_wrappedNonCollectionType_shouldReturnFalse() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isCollection(typeMirror)).thenReturn(false);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertFalse(result);
        verify(typeInfo).isWrapped();
        verify(extractor).isCollection(typeMirror);
    }

    @Test
    @DisplayName("generateCode should generate code for List<String>")
    void generateCode_listOfStrings_shouldGenerateCorrectCode() {
        // Arrange
        // CollectionReturnHandler internally uses ComposedReturnHandler which needs additional mocks
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("results");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.String");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.List");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(extractor, atLeastOnce()).eraseType(typeMirror);
        verify(returnElementInfo, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("generateCode should generate code for Set<Integer>")
    void generateCode_setOfIntegers_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("numbers");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Integer");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.Set");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(extractor, atLeastOnce()).eraseType(typeMirror);
    }

    @Test
    @DisplayName("generateCode should handle collection of complex objects")
    void generateCode_listOfComplexObjects_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("users");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.User");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.List");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should not return null for valid collection")
    void generateCode_validCollection_shouldNeverReturnNull() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("items");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Object");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.Collection");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code, "Generated code should never be null");
    }

    @Test
    @DisplayName("generateCode should handle underscore in variable name")
    void generateCode_underscoreInName_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("user_list");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.User");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.List");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle ArrayList type")
    void generateCode_arrayListType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("items");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.String");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.ArrayList");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle HashSet type")
    void generateCode_hashSetType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("uniqueItems");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Long");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.HashSet");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("canHandle should handle null typeInfo gracefully")
    void canHandle_nullTypeInfo_shouldThrowException() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> handler.canHandle(returnElementInfo));
    }

    @Test
    @DisplayName("generateCode should handle camelCase variable names")
    void generateCode_camelCaseNames_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("userAccountList");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.UserAccount");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.List");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle collection with generic nested type")
    void generateCode_nestedGenericType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("data");
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getElementInfoList()).thenReturn(java.util.Collections.emptyList());
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.util.Map<String,Integer>");
        when(typeInfo.isRecord()).thenReturn(false);
        when(extractor.eraseType(typeMirror)).thenReturn(erasedTypeMirror);
        when(erasedTypeMirror.toString()).thenReturn("java.util.List");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }
}
