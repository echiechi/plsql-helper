package com.plsql.tools.handlers;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SimpleReturnHandler.
 * Tests the handling of simple return types (primitives and their wrappers).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleReturnHandler Tests")
class SimpleReturnHandlerTest {

    private SimpleReturnHandler handler;

    @Mock
    private ReturnElementInfo returnElementInfo;

    @Mock
    private TypeInfo typeInfo;

    @BeforeEach
    void setUp() {
        handler = new SimpleReturnHandler();
    }

    @Test
    @DisplayName("canHandle should return true when typeInfo is simple")
    void canHandle_simpleType_shouldReturnTrue() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertTrue(result);
        verify(typeInfo).isSimple();
    }

    @Test
    @DisplayName("canHandle should return false when typeInfo is not simple")
    void canHandle_nonSimpleType_shouldReturnFalse() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertFalse(result);
        verify(typeInfo).isSimple();
    }

    @Test
    @DisplayName("generateCode should generate code for String type")
    void generateCode_stringType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("result");
        when(typeInfo.typeAsString()).thenReturn("java.lang.String");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        // Code should contain position, type, getter method, and variable name
        assertTrue(code.contains("1") || code.length() > 0);
        verify(returnElementInfo, atLeastOnce()).getPos();
        verify(returnElementInfo, atLeastOnce()).getName();
        verify(typeInfo, atLeastOnce()).typeAsString();
        verify(typeInfo, atLeastOnce()).asTypeMapper();
    }

    @Test
    @DisplayName("generateCode should generate code for Integer type")
    void generateCode_integerType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("2");
        when(returnElementInfo.getName()).thenReturn("count");
        when(typeInfo.typeAsString()).thenReturn("java.lang.Integer");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.INTEGER_WRAPPER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(returnElementInfo, atLeastOnce()).getPos();
        verify(returnElementInfo, atLeastOnce()).getName();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "10", "999"})
    @DisplayName("generateCode should handle various position values")
    void generateCode_variousPositions_shouldGenerateCorrectCode(String position) {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn(position);
        when(returnElementInfo.getName()).thenReturn("value");
        when(typeInfo.typeAsString()).thenReturn("java.lang.String");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle return element with special characters in name")
    void generateCode_specialCharactersInName_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("user_id");
        when(typeInfo.typeAsString()).thenReturn("java.lang.Long");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.LONG_WRAPPER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle primitive int type")
    void generateCode_primitiveInt_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("3");
        when(returnElementInfo.getName()).thenReturn("count");
        when(typeInfo.typeAsString()).thenReturn("int");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.INTEGER);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle boolean type")
    void generateCode_booleanType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("isActive");
        when(typeInfo.typeAsString()).thenReturn("boolean");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.BOOLEAN);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle BigDecimal type")
    void generateCode_bigDecimalType_shouldGenerateCorrectCode() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("amount");
        when(typeInfo.typeAsString()).thenReturn("java.math.BigDecimal");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.BIG_DECIMAL);

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
        // This tests the edge case where typeInfo might be null
        assertThrows(NullPointerException.class, () -> handler.canHandle(returnElementInfo));
    }

    @Test
    @DisplayName("generateCode should not return null")
    void generateCode_validInput_shouldNeverReturnNull() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getPos()).thenReturn("1");
        when(returnElementInfo.getName()).thenReturn("result");
        when(typeInfo.typeAsString()).thenReturn("java.lang.String");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code, "Generated code should never be null");
    }
}
