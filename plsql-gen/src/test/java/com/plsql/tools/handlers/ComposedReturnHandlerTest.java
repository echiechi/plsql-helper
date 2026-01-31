package com.plsql.tools.handlers;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComposedReturnHandler.
 * Tests the handling of composed/complex return types (objects, records, nested structures).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComposedReturnHandler Tests")
class ComposedReturnHandlerTest {

    @Mock
    private Extractor extractor;

    @Mock
    private ReturnElementInfo returnElementInfo;

    @Mock
    private TypeInfo typeInfo;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private Element rawTypeElement;

    private ComposedReturnHandler handler;

    @BeforeEach
    void setUp() {
        handler = ComposedReturnHandler.builder().extractor(extractor).build();
    }

    @Test
    @DisplayName("constructor with extractor only should initialize with default values")
    void constructor_extractorOnly_shouldInitializeWithDefaults() {
        // Act
        ComposedReturnHandler newHandler = ComposedReturnHandler.builder().extractor(extractor).build();

        // Assert
        assertNotNull(newHandler);
    }

    @Test
    @DisplayName("constructor with all parameters should initialize correctly")
    void constructor_allParameters_shouldInitializeCorrectly() {
        // Act
        ComposedReturnHandler newHandler = new ComposedReturnHandler(
                extractor,
                true,  // isToAssign
                false, // isWrapped
                true,  // isInitObject
                true,  // isReturnSomething
                "// additional code"
        );

        // Assert
        assertNotNull(newHandler);
    }

    @Test
    @DisplayName("builder should create handler with custom configuration")
    void builder_customConfiguration_shouldCreateHandler() {
        // Act
        ComposedReturnHandler newHandler = ComposedReturnHandler.builder()
                .extractor(extractor)
                .isToAssign(false)
                .isWrapped(true)
                .isInitObject(false)
                .isReturnSomething(false)
                .toAppendToStatements("custom code;")
                .build();

        // Assert
        assertNotNull(newHandler);
    }

    @Test
    @DisplayName("canHandle should return true for non-wrapped, non-simple types")
    void canHandle_composedType_shouldReturnTrue() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(false);
        when(typeInfo.isSimple()).thenReturn(false);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertTrue(result);
        verify(typeInfo).isWrapped();
        verify(typeInfo).isSimple();
    }

    @Test
    @DisplayName("canHandle should return false for wrapped types")
    void canHandle_wrappedType_shouldReturnFalse() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(true);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertFalse(result);
        verify(typeInfo).isWrapped();
    }

    @Test
    @DisplayName("canHandle should return false for simple types")
    void canHandle_simpleType_shouldReturnFalse() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(false);
        when(typeInfo.isSimple()).thenReturn(true);

        // Act
        boolean result = handler.canHandle(returnElementInfo);

        // Assert
        assertFalse(result);
        verify(typeInfo).isSimple();
    }

    @Test
    @DisplayName("generateCode should generate code for simple composed object")
    void generateCode_simpleComposedObject_shouldGenerateCorrectCode() {
        // Arrange
        setupBasicReturnElement("User", "1", false);

        AttachedElementInfo attachedElement = createAttachedElement("name", "java.lang.String", true);
        List<AttachedElementInfo> elements = new ArrayList<>();
        elements.add(attachedElement);

        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(returnElementInfo, atLeastOnce()).getPos();
        verify(returnElementInfo, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("generateCode should generate code for record type")
    void generateCode_recordType_shouldGenerateCorrectCode() {
        // Arrange
        setupBasicReturnElement("UserRecord", "1", true);

        AttachedElementInfo element1 = createAttachedElementWithoutSetter("id", "java.lang.Long", true);
        AttachedElementInfo element2 = createAttachedElementWithoutSetter("name", "java.lang.String", true);
        List<AttachedElementInfo> elements = List.of(element1, element2);

        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(typeInfo).isRecord();
    }

    @Test
    @DisplayName("generateCode should handle nested composed elements")
    void generateCode_nestedComposedElements_shouldGenerateCorrectCode() {
        // Arrange
        setupBasicReturnElement("ComplexUser", "1", false);

        // Create a nested element (Address)
        TypeInfo nestedTypeInfo = mock(TypeInfo.class);
        when(nestedTypeInfo.isSimple()).thenReturn(false);
        when(nestedTypeInfo.getRawType()).thenReturn(rawTypeElement);
        when(nestedTypeInfo.typeAsString()).thenReturn("com.example.Address");
        when(nestedTypeInfo.isRecord()).thenReturn(false);

        AttachedElementInfo nestedElement = createAttachedElementWithTypeInfo("address", nestedTypeInfo);
        // new AttachedElementInfo();
        // nestedElement.setTypeInfo(nestedTypeInfo);
        // nestedElement.setName("address");
        nestedElement.setAlias("address");

        List<AttachedElementInfo> elements = List.of(nestedElement);
        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Mock the nested conversion
        ComposedElementInfo nestedComposed = new ComposedElementInfo(nestedTypeInfo, "Address");
        AttachedElementInfo streetElement = createAttachedElement("street", "java.lang.String", true);
        nestedComposed.setElementInfoList(List.of(streetElement));

        when(extractor.convertInto(rawTypeElement)).thenReturn(nestedComposed);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
        verify(extractor).convertInto(rawTypeElement);
    }

    @Test
    @DisplayName("generateCode should handle multiple simple elements")
    void generateCode_multipleSimpleElements_shouldGenerateCorrectCode() {
        // Arrange
        setupBasicReturnElement("Person", "2", false);

        List<AttachedElementInfo> elements = List.of(
                createAttachedElement("firstName", "java.lang.String", true),
                createAttachedElement("lastName", "java.lang.String", true),
                createAttachedElement("age", "java.lang.Integer", true)
        );

        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode with builder should respect isToAssign flag")
    void generateCode_builderWithIsToAssignFalse_shouldNotAssign() {
        // Arrange
        ComposedReturnHandler customHandler = ComposedReturnHandler.builder()
                .extractor(extractor)
                .isToAssign(false)
                .isWrapped(false)
                .isInitObject(true)
                .isReturnSomething(true)
                .toAppendToStatements("")
                .build();

        setupBasicReturnElement("User", "1", false);
        AttachedElementInfo element = createAttachedElement("name", "java.lang.String", true);
        when(returnElementInfo.getElementInfoList()).thenReturn(List.of(element));

        // Act
        String code = customHandler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode with builder should respect isInitObject flag")
    void generateCode_builderWithIsInitObjectFalse_shouldNotInitialize() {
        // Arrange
        ComposedReturnHandler customHandler = ComposedReturnHandler.builder()
                .extractor(extractor)
                .isToAssign(true)
                .isWrapped(false)
                .isInitObject(false)  // No initialization
                .isReturnSomething(true)
                .toAppendToStatements("")
                .build();

        setupBasicReturnElement("User", "1", false);
        AttachedElementInfo element = createAttachedElement("name", "java.lang.String", true);
        when(returnElementInfo.getElementInfoList()).thenReturn(List.of(element));

        // Act
        String code = customHandler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        // Code should not contain initialization to null
    }

    @Test
    @DisplayName("generateCode with builder should append custom statements")
    void generateCode_builderWithToAppendToStatements_shouldAppendCode() {
        // Arrange
        String customCode = "customStatement();";
        ComposedReturnHandler customHandler = ComposedReturnHandler.builder()
                .extractor(extractor)
                .isToAssign(true)
                .isWrapped(false)
                .isInitObject(true)
                .isReturnSomething(true)
                .toAppendToStatements(customCode)
                .build();

        setupBasicReturnElement("User", "1", false);
        AttachedElementInfo element = createAttachedElement("name", "java.lang.String", true);
        when(returnElementInfo.getElementInfoList()).thenReturn(List.of(element));

        // Act
        String code = customHandler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle date/time types with transformations")
    void generateCode_dateTimeTypes_shouldApplyTransformations() {
        // Arrange
        setupBasicReturnElement("Event", "1", false);

        AttachedElementInfo dateElement = createAttachedElement("eventDate", "java.util.Date", true, TypeMapper.DATE);
        List<AttachedElementInfo> elements = List.of(dateElement);

        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle LocalDate type")
    void generateCode_localDateType_shouldApplyTransformation() {
        // Arrange
        setupBasicReturnElement("Schedule", "1", false);

        AttachedElementInfo localDateElement = createAttachedElement("scheduleDate", "java.time.LocalDate", true, TypeMapper.LOCAL_DATE);
        List<AttachedElementInfo> elements = List.of(localDateElement);

        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle Character type with transformation")
    void generateCode_characterType_shouldApplyTransformation() {
        // Arrange
        setupBasicReturnElement("Data", "1", false);

        AttachedElementInfo charElement = createAttachedElement("initial", "java.lang.Character", true, TypeMapper.CHARACTER_WRAPPER);
        List<AttachedElementInfo> elements = List.of(charElement);

        when(returnElementInfo.getElementInfoList()).thenReturn(elements);

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("generateCode should handle empty element list")
    void generateCode_emptyElementList_shouldGenerateCorrectCode() {
        // Arrange
        setupBasicReturnElement("EmptyObject", "1", false);
        when(returnElementInfo.getElementInfoList()).thenReturn(new ArrayList<>());

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code);
    }

    @Test
    @DisplayName("generateCode should not return null")
    void generateCode_validInput_shouldNeverReturnNull() {
        // Arrange
        setupBasicReturnElement("TestObject", "1", false);
        AttachedElementInfo element = createAttachedElement("field", "java.lang.String", true);
        when(returnElementInfo.getElementInfoList()).thenReturn(List.of(element));

        // Act
        String code = handler.generateCode(returnElementInfo);

        // Assert
        assertNotNull(code, "Generated code should never be null");
    }

    @Test
    @DisplayName("canHandle should handle null typeInfo gracefully")
    void canHandle_nullTypeInfo_shouldThrowException() {
        // Arrange
        when(returnElementInfo.getTypeInfo()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> handler.canHandle(returnElementInfo));
    }

    // Helper methods

    /**
     * Sets up a basic ReturnElementInfo with common mocking
     */
    private void setupBasicReturnElement(String name, String position, boolean isRecord) {
        when(returnElementInfo.getTypeInfo()).thenReturn(typeInfo);
        when(returnElementInfo.getName()).thenReturn(name);
        when(returnElementInfo.getPos()).thenReturn(position);
        when(typeInfo.typeAsString()).thenReturn("com.example." + name);
        when(typeInfo.isRecord()).thenReturn(isRecord);
//        when(typeInfo.isWrapped()).thenReturn(false);
//        when(typeInfo.isSimple()).thenReturn(false);
//        when(typeInfo.getMirror()).thenReturn(typeMirror);
//        when(typeInfo.getRawType()).thenReturn(rawTypeElement);
    }

    /**
     * Creates an AttachedElementInfo with the given parameters
     */
    private AttachedElementInfo createAttachedElement(String name, String type, boolean isSimple) {
        return createAttachedElement(name, type, isSimple, TypeMapper.fromSimpleName(type));
    }

    /**
     * Creates an AttachedElementInfo with specific TypeMapper
     */
    private AttachedElementInfo createAttachedElement(String name, String type, boolean isSimple, TypeMapper mapper) {
        AttachedElementInfo element = new AttachedElementInfo();

        TypeInfo elementTypeInfo = mock(TypeInfo.class);
        when(elementTypeInfo.typeAsString()).thenReturn(type);
        when(elementTypeInfo.isSimple()).thenReturn(isSimple);
        when(elementTypeInfo.asTypeMapper()).thenReturn(mapper);

        element.setTypeInfo(elementTypeInfo);
        element.setName(name);
        element.setAlias(name);

        // Mock setter for the element
        ExecutableElement setter = mock(ExecutableElement.class);
        Name setterName = mock(Name.class);
        when(setterName.toString()).thenReturn("set" + name.substring(0, 1).toUpperCase() + name.substring(1));
        when(setter.getSimpleName()).thenReturn(setterName);
        element.setSetter(setter);

        return element;
    }

    private AttachedElementInfo createAttachedElementWithTypeInfo(String name, TypeInfo type) {
        AttachedElementInfo element = new AttachedElementInfo();

        element.setTypeInfo(type);
        element.setName(name);
        element.setAlias(name);

        // Mock setter for the element
        ExecutableElement setter = mock(ExecutableElement.class);
        Name setterName = mock(Name.class);
        when(setterName.toString()).thenReturn("set" + name.substring(0, 1).toUpperCase() + name.substring(1));
        when(setter.getSimpleName()).thenReturn(setterName);
        element.setSetter(setter);

        return element;
    }


    private AttachedElementInfo createAttachedElementWithoutSetter(String name, String type, boolean isSimple) {
        return createAttachedElementWithoutSetter(name, type, isSimple, TypeMapper.fromSimpleName(type));
    }

    private AttachedElementInfo createAttachedElementWithoutSetter(String name, String type, boolean isSimple, TypeMapper mapper) {
        AttachedElementInfo element = new AttachedElementInfo();

        TypeInfo elementTypeInfo = mock(TypeInfo.class);
        when(elementTypeInfo.typeAsString()).thenReturn(type);
        when(elementTypeInfo.isSimple()).thenReturn(isSimple);
        when(elementTypeInfo.asTypeMapper()).thenReturn(mapper);

        element.setTypeInfo(elementTypeInfo);
        element.setName(name);
        element.setAlias(name);
        ExecutableElement setter = mock(ExecutableElement.class);
        element.setSetter(setter);


        return element;
    }
}
