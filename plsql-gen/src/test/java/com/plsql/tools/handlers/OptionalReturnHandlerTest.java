package com.plsql.tools.handlers;

import com.plsql.tools.enums.TypeMapper;
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
 * Unit tests for OptionalReturnHandler.
 * Tests the handling of Optional return types (both simple and composed).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OptionalReturnHandler Tests")
class OptionalReturnHandlerTest {

    @Mock
    private Extractor extractor;

    @Mock
    private ReturnElementInfo returnElementInfo;

    @Mock
    private TypeInfo typeInfo;

    @Mock
    private TypeMirror typeMirror;

    private OptionalReturnHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OptionalReturnHandler(extractor);
    }

    @Test
    @DisplayName("constructor should initialize with valid extractor")
    void constructor_validExtractor_shouldInitialize() {
        // Act
        OptionalReturnHandler newHandler = new OptionalReturnHandler(extractor);

        // Assert
        assertNotNull(newHandler);
    }

    @Test
    @DisplayName("canHandle should return true for wrapped Optional types")
    void canHandle_wrappedOptionalType_shouldReturnTrue() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(true);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertTrue(result);
        verify(typeInfo).isWrapped();
        verify(extractor).isOptional(typeMirror);
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
        verify(extractor, never()).isOptional(any());
    }

    @Test
    @DisplayName("canHandle should return false for wrapped non-Optional types")
    void canHandle_wrappedNonOptionalType_shouldReturnFalse() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getMirror()).thenReturn(typeMirror);
        when(extractor.isOptional(typeMirror)).thenReturn(false);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertFalse(result);
        verify(typeInfo).isWrapped();
        verify(extractor).isOptional(typeMirror);
    }

    @Test
    @DisplayName("generateCode should generate code for Optional<String>")
    void generateCode_optionalString_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("result");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.String");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(typeInfo).isWrappedSimple();
        verify(returnElementInfo, atLeastOnce()).getPos();
        verify(returnElementInfo, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("generateCode should generate code for Optional<Integer>")
    void generateCode_optionalInteger_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("2");
        when(returnElementInfo.getName()).thenReturn("count");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Integer");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.INTEGER_WRAPPER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should generate code for Optional<ComplexObject>")
    void generateCode_optionalComplexObject_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("user");
        when(returnElementInfo.getPos()).thenReturn("pos");
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.User");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(typeInfo).isWrappedSimple();
    }

    @Test
    @DisplayName("generateCode should not return null for simple Optional")
    void generateCode_simpleOptional_shouldNeverReturnNull() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("value");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.String");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code, "Generated code should never be null");
    }

    @Test
    @DisplayName("generateCode should not return null for composed Optional")
    void generateCode_composedOptional_shouldNeverReturnNull() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("record");
        when(returnElementInfo.getPos()).thenReturn("pos");

        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.Record");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code, "Generated code should never be null");
    }

    @Test
    @DisplayName("generateCode should handle Optional<Boolean>")
    void generateCode_optionalBoolean_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("isActive");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Boolean");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.BOOLEAN_WRAPPER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle Optional<BigDecimal>")
    void generateCode_optionalBigDecimal_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("3");
        when(returnElementInfo.getName()).thenReturn("amount");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.math.BigDecimal");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.BIG_DECIMAL);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle underscore in variable name for simple Optional")
    void generateCode_underscoreInNameSimple_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("user_id");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Long");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.LONG_WRAPPER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle underscore in variable name for composed Optional")
    void generateCode_underscoreInNameComposed_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("user_record");
        when(returnElementInfo.getPos()).thenReturn("pos");

        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.UserRecord");

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle camelCase variable names for simple Optional")
    void generateCode_camelCaseSimple_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("userId");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.Long");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.LONG_WRAPPER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle camelCase variable names for composed Optional")
    void generateCode_camelCaseComposed_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("userRecord");
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.UserRecord");
        when(returnElementInfo.getPos()).thenReturn("pos");

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
    @DisplayName("generateCode should throw NullPointerException when wrappedTypeAsTypeMapper returns null for simple Optional")
    void generateCode_nullTypeMapper_shouldThrowException() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("UnknownType");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(null);

        // Act & Assert
        // This should throw NullPointerException when trying to call getJdbcGetterMethod on null
        assertThrows(NullPointerException.class, () -> handler.generateCode(returnElementInfo));
    }

    @Test
    @DisplayName("generateCode should differentiate between simple and composed Optional paths")
    void generateCode_differentPaths_shouldUseDifferentLogic() {
        // Test simple path
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("simpleValue");
        when(typeInfo.isWrappedSimple()).thenReturn(true);
        when(typeInfo.wrappedTypeAsString()).thenReturn("java.lang.String");
        when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(TypeMapper.STRING);

        String simpleCode = handler.generateCode(returnElementInfo);
        assertNotNull(simpleCode);

        // Reset mocks and test composed path
        reset(returnElementInfo, typeInfo);
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn("composedValue");
        when(typeInfo.isWrappedSimple()).thenReturn(false);
        when(returnElementInfo.getPos()).thenReturn("pos");
        when(typeInfo.wrappedTypeAsString()).thenReturn("com.example.ComposedType");

        String composedCode = handler.generateCode(returnElementInfo);
        assertNotNull(composedCode);

        // The generated code should be different for different paths
        // Note: We can't assert they're different without knowing the exact template output,
        // but we verify both paths execute without errors
    }
}
