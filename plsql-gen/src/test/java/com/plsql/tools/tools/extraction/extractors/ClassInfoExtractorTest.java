package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassInfoExtractor Tests")
class ClassInfoExtractorTest {

    @Mock
    private TypeInfoExtractor typeInfoExtractor;

    @Mock
    private ProcessingContext context;

    @Mock
    private Element classElement;

    @Mock
    private Element recordElement;

    /*@Mock
    private Element fieldElement;

    @Mock
    private ExecutableElement methodElement;

    @Mock
    private ExecutableElement constructorElement;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private Name elementName;
*/
    private ClassInfoExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ClassInfoExtractor(typeInfoExtractor, context);
    }

    @Test
    @DisplayName("extractClassInfo should throw exception when element is null")
    void testExtractClassInfo_nullElement() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extractClassInfo(null)
        );

        assertEquals("Class or Record element cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("extractClassInfo should throw exception for non-class/record elements")
    void testExtractClassInfo_invalidElementKind() {
        // Arrange
        when(classElement.getKind()).thenReturn(ElementKind.INTERFACE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractor.extractClassInfo(classElement)
        );

        assertTrue(exception.getMessage().contains("Element must be a CLASS or RECORD"));
    }

    @Test
    @DisplayName("extractClassInfo should throw exception when class has no default constructor")
    void testExtractClassInfo_classWithoutDefaultConstructor() {
        // Arrange
        ExecutableElement constructorWithParams = mock(ExecutableElement.class);
        VariableElement param = mock(VariableElement.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(constructorWithParams));
        when(constructorWithParams.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructorWithParams.getParameters()).thenAnswer(inv -> List.of(param));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> extractor.extractClassInfo(classElement)
        );

        assertEquals("No default constructor is present", exception.getMessage());
    }

    @Test
    @DisplayName("extractClassInfo should successfully extract info from class with default constructor")
    void testExtractClassInfo_classWithDefaultConstructor() {
        // Arrange
        Element field = mock(Element.class);
        ExecutableElement getter = mock(ExecutableElement.class);
        ExecutableElement constructor = mock(ExecutableElement.class);
        Name fieldName = mock(Name.class);
        Name getterName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(field, getter, constructor));

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getModifiers()).thenReturn(Collections.emptySet());
        when(fieldName.toString()).thenReturn("userName");

        when(getter.getKind()).thenReturn(ElementKind.METHOD);
        when(getter.getSimpleName()).thenReturn(getterName);
       // when(getter.getParameters()).thenReturn(Collections.emptyList());
        when(getterName.toString()).thenReturn("getUserName");

        when(constructor.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        when(typeInfoExtractor.extractTypeInfo(field)).thenReturn(typeInfo);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(classElement);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        AttachedElementInfo attachedElement = result.get(0);
        assertEquals("userName", attachedElement.getName());
        assertEquals(typeInfo, attachedElement.getTypeInfo());
        assertEquals(getter, attachedElement.getGetter());
    }

    @Test
    @DisplayName("extractClassInfo should handle record elements")
    void testExtractClassInfo_recordElement() {
        // Arrange
        Element field = mock(Element.class);
        ExecutableElement accessor = mock(ExecutableElement.class);
        Name fieldName = mock(Name.class);
        Name accessorName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(recordElement.getKind()).thenReturn(ElementKind.RECORD);
        when(recordElement.getEnclosedElements()).thenAnswer(inv -> List.of(field, accessor));

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(fieldName.toString()).thenReturn("id");

        when(accessor.getKind()).thenReturn(ElementKind.METHOD);
        when(accessor.getSimpleName()).thenReturn(accessorName);
//        when(accessor.getParameters()).thenReturn(Collections.emptyList());
        when(accessorName.toString()).thenReturn("id");

        when(typeInfoExtractor.extractTypeInfo(field)).thenReturn(typeInfo);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(recordElement);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        AttachedElementInfo attachedElement = result.get(0);
        assertEquals("id", attachedElement.getName());
        assertEquals(typeInfo, attachedElement.getTypeInfo());
        assertEquals(accessor, attachedElement.getGetter());
    }

    @Test
    @DisplayName("extractClassInfo should handle class with setter methods")
    void testExtractClassInfo_classWithSetter() {
        // Arrange
        Element field = mock(Element.class);
        ExecutableElement getter = mock(ExecutableElement.class);
        ExecutableElement setter = mock(ExecutableElement.class);
        ExecutableElement constructor = mock(ExecutableElement.class);
        Name fieldName = mock(Name.class);
        Name getterName = mock(Name.class);
        Name setterName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);
        VariableElement setterParam = mock(VariableElement.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(field, getter, setter, constructor));

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getModifiers()).thenReturn(Collections.emptySet());
        when(fieldName.toString()).thenReturn("email");

        when(getter.getKind()).thenReturn(ElementKind.METHOD);
        when(getter.getSimpleName()).thenReturn(getterName);
//        when(getter.getParameters()).thenReturn(Collections.emptyList());
        when(getterName.toString()).thenReturn("getEmail");

        when(setter.getKind()).thenReturn(ElementKind.METHOD);
        when(setter.getSimpleName()).thenReturn(setterName);
//        when(setter.getParameters()).thenAnswer(inv -> List.of(setterParam));
        when(setterName.toString()).thenReturn("setEmail");

        when(constructor.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        when(typeInfoExtractor.extractTypeInfo(field)).thenReturn(typeInfo);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(classElement);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        AttachedElementInfo attachedElement = result.get(0);
        assertEquals(getter, attachedElement.getGetter());
        assertEquals(setter, attachedElement.getSetter());
    }

    @Test
    @DisplayName("extractClassInfo should handle public field without getter")
    void testExtractClassInfo_publicFieldWithoutGetter() {
        // Arrange
        Element field = mock(Element.class);
        ExecutableElement constructor = mock(ExecutableElement.class);
        Name fieldName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(field, constructor));

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getModifiers()).thenReturn(java.util.Set.of(Modifier.PUBLIC));
        when(fieldName.toString()).thenReturn("publicField");

        when(constructor.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        when(typeInfoExtractor.extractTypeInfo(field)).thenReturn(typeInfo);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(classElement);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        AttachedElementInfo attachedElement = result.get(0);
        assertTrue(attachedElement.isPublic());
        assertNull(attachedElement.getGetter());
        verify(context).logWarning(contains("Using direct field access"));
    }

    @Test
    @DisplayName("extractClassInfo should warn about private field without getter")
    void testExtractClassInfo_privateFieldWithoutGetter() {
        // Arrange
        Element field = mock(Element.class);
        ExecutableElement constructor = mock(ExecutableElement.class);
        Name fieldName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(field, constructor));

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.getModifiers()).thenReturn(Collections.emptySet());
        when(fieldName.toString()).thenReturn("privateField");

        when(constructor.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        when(typeInfoExtractor.extractTypeInfo(field)).thenReturn(typeInfo);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(classElement);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(context).logWarning(contains("Skipping private field without getter"));
    }

    @Test
    @DisplayName("extractClassInfo should handle class with no fields")
    void testExtractClassInfo_classWithNoFields() {
        // Arrange
        ExecutableElement constructor = mock(ExecutableElement.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(constructor));

        when(constructor.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(classElement);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractClassInfo should handle class with multiple fields")
    void testExtractClassInfo_classWithMultipleFields() {
        // Arrange
        Element field1 = mock(Element.class);
        Element field2 = mock(Element.class);
        ExecutableElement constructor = mock(ExecutableElement.class);
        Name field1Name = mock(Name.class);
        Name field2Name = mock(Name.class);
        TypeInfo typeInfo1 = mock(TypeInfo.class);
        TypeInfo typeInfo2 = mock(TypeInfo.class);

        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(field1, field2, constructor));

        when(field1.getKind()).thenReturn(ElementKind.FIELD);
        when(field1.getSimpleName()).thenReturn(field1Name);
        when(field1.getModifiers()).thenReturn(java.util.Set.of(Modifier.PUBLIC));
        when(field1Name.toString()).thenReturn("field1");

        when(field2.getKind()).thenReturn(ElementKind.FIELD);
        when(field2.getSimpleName()).thenReturn(field2Name);
        when(field2.getModifiers()).thenReturn(java.util.Set.of(Modifier.PUBLIC));
        when(field2Name.toString()).thenReturn("field2");

        when(constructor.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor.getParameters()).thenReturn(Collections.emptyList());

        when(typeInfoExtractor.extractTypeInfo(field1)).thenReturn(typeInfo1);
        when(typeInfoExtractor.extractTypeInfo(field2)).thenReturn(typeInfo2);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(classElement);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("field1", result.get(0).getName());
        assertEquals("field2", result.get(1).getName());
    }

    @Test
    @DisplayName("extractClassInfo should handle record with no accessor method")
    void testExtractClassInfo_recordWithoutAccessor() {
        // Arrange
        Element field = mock(Element.class);
        Name fieldName = mock(Name.class);
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(recordElement.getKind()).thenReturn(ElementKind.RECORD);
        when(recordElement.getEnclosedElements()).thenAnswer(inv -> List.of(field));

        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(fieldName.toString()).thenReturn("value");

        when(typeInfoExtractor.extractTypeInfo(field)).thenReturn(typeInfo);

        // Act
        List<AttachedElementInfo> result = extractor.extractClassInfo(recordElement);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getGetter());
    }

    @Test
    @DisplayName("extractConstructors should extract all constructors")
    void testExtractConstructors() {
        // Arrange
        ExecutableElement constructor1 = mock(ExecutableElement.class);
        ExecutableElement constructor2 = mock(ExecutableElement.class);
        ExecutableElement method = mock(ExecutableElement.class);

        when(classElement.getEnclosedElements()).thenAnswer(inv -> List.of(constructor1, method, constructor2));
        when(constructor1.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(constructor2.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when(method.getKind()).thenReturn(ElementKind.METHOD);

        // Act
        List<ExecutableElement> result = extractor.extractConstructors(classElement);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(constructor1));
        assertTrue(result.contains(constructor2));
        assertFalse(result.contains(method));
    }

    @Test
    @DisplayName("extractClassInfo should handle enum elements by throwing exception")
    void testExtractClassInfo_enumElement() {
        // Arrange
        when(classElement.getKind()).thenReturn(ElementKind.ENUM);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> extractor.extractClassInfo(classElement));
    }
}
