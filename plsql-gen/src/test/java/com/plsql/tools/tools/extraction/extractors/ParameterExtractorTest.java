package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.tools.extraction.cache.SimpleCache;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParameterExtractor Tests")
class ParameterExtractorTest {

    @Mock
    private TypeInfoExtractor typeInfoExtractor;

    @Mock
    private ComposedElementExtractor composedElementExtractor;

    @Mock
    private SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    @Mock
    private ExecutableElement method;

    @Mock
    private VariableElement parameter;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private TypeInfo typeInfo;

    @Mock
    private Name parameterName;

    private ParameterExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ParameterExtractor(typeInfoExtractor, composedElementExtractor, cache);
    }

    @Test
    @DisplayName("extractParams should return empty list for method with no parameters")
    void testExtractParams_noParameters() {
        // Arrange
        when(method.getParameters()).thenReturn(Collections.emptyList());

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractParams should extract simple parameter")
    void testExtractParams_simpleParameter() {
        // Arrange
        when(method.getParameters()).thenAnswer(inv -> List.of(parameter));
        when(parameter.getSimpleName()).thenReturn(parameterName);
        when(parameterName.toString()).thenReturn("userId");
        when(typeInfoExtractor.extractTypeInfo(parameter)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        ElementInfo elementInfo = result.get(0);
        assertEquals("userId", elementInfo.getName());
        assertEquals(typeInfo, elementInfo.getTypeInfo());
    }

    @Test
    @DisplayName("extractParams should extract complex parameter using ComposedElementExtractor")
    void testExtractParams_complexParameter() {
        // Arrange
        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);

        when(method.getParameters()).thenAnswer(inv -> List.of(parameter));
        when(parameter.getSimpleName()).thenReturn(parameterName);
        when(parameterName.toString()).thenReturn("user");
        when(typeInfoExtractor.extractTypeInfo(parameter)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(composedElementExtractor.convertInto(parameter)).thenReturn(composedInfo);

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(composedInfo, result.get(0));
        verify(composedElementExtractor).convertInto(parameter);
    }

    @Test
    @DisplayName("extractParams should handle multiple parameters")
    void testExtractParams_multipleParameters() {
        // Arrange
        VariableElement param1 = mock(VariableElement.class);
        VariableElement param2 = mock(VariableElement.class);
        Name param1Name = mock(Name.class);
        Name param2Name = mock(Name.class);
        TypeInfo typeInfo1 = mock(TypeInfo.class);
        TypeInfo typeInfo2 = mock(TypeInfo.class);

        when(method.getParameters()).thenAnswer(inv -> List.of(param1, param2));

        when(param1.getSimpleName()).thenReturn(param1Name);
        when(param1Name.toString()).thenReturn("firstName");
        when(typeInfoExtractor.extractTypeInfo(param1)).thenReturn(typeInfo1);
        when(typeInfo1.isSimple()).thenReturn(true);

        when(param2.getSimpleName()).thenReturn(param2Name);
        when(param2Name.toString()).thenReturn("lastName");
        when(typeInfoExtractor.extractTypeInfo(param2)).thenReturn(typeInfo2);
        when(typeInfo2.isSimple()).thenReturn(true);

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("firstName", result.get(0).getName());
        assertEquals("lastName", result.get(1).getName());
    }

    @Test
    @DisplayName("extractParams should handle mix of simple and complex parameters")
    void testExtractParams_mixedParameters() {
        // Arrange
        VariableElement simpleParam = mock(VariableElement.class);
        VariableElement complexParam = mock(VariableElement.class);
        Name simpleName = mock(Name.class);
        Name complexName = mock(Name.class);
        TypeInfo simpleTypeInfo = mock(TypeInfo.class);
        TypeInfo complexTypeInfo = mock(TypeInfo.class);
        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);

        when(method.getParameters()).thenAnswer(inv -> List.of(simpleParam, complexParam));

        when(simpleParam.getSimpleName()).thenReturn(simpleName);
        when(simpleName.toString()).thenReturn("id");
        when(typeInfoExtractor.extractTypeInfo(simpleParam)).thenReturn(simpleTypeInfo);
        when(simpleTypeInfo.isSimple()).thenReturn(true);

        when(complexParam.getSimpleName()).thenReturn(complexName);
        when(complexName.toString()).thenReturn("address");
        when(typeInfoExtractor.extractTypeInfo(complexParam)).thenReturn(complexTypeInfo);
        when(complexTypeInfo.isSimple()).thenReturn(false);
        when(composedElementExtractor.convertInto(complexParam)).thenReturn(composedInfo);

        // Act
        List<ElementInfo> result = extractor.extractParams(method);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof ElementInfo);
        assertEquals(composedInfo, result.get(1));
    }

    @Test
    @DisplayName("extractPramNames should return empty list for empty input")
    void testExtractPramNames_emptyList() {
        // Act
        List<String> result = extractor.extractPramNames(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractPramNames should extract names from simple elements")
    void testExtractPramNames_simpleElements() {
        // Arrange
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
    @DisplayName("extractPramNames should flatten nested parameter names")
    void testExtractPramNames_nestedElements() {
        // Arrange
        ElementInfo element = mock(ElementInfo.class);
        TypeInfo complexTypeInfo = mock(TypeInfo.class);
        TypeMirror complexTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo nestedElement1 = mock(AttachedElementInfo.class);
        AttachedElementInfo nestedElement2 = mock(AttachedElementInfo.class);
        TypeInfo nestedTypeInfo1 = mock(TypeInfo.class);
        TypeInfo nestedTypeInfo2 = mock(TypeInfo.class);

        when(nestedTypeInfo1.getRawType()).thenReturn(mock(Element.class));
        when(nestedTypeInfo2.getRawType()).thenReturn(mock(Element.class));

        when(element.getTypeInfo()).thenReturn(complexTypeInfo);
        when(complexTypeInfo.isSimple()).thenReturn(false);
        when(complexTypeInfo.getMirror()).thenReturn(complexTypeMirror);

        when(cache.get(complexTypeMirror)).thenReturn(Optional.of(List.of(nestedElement1, nestedElement2)));

        when(nestedElement1.getName()).thenReturn("street");
        when(nestedElement1.getTypeInfo()).thenReturn(nestedTypeInfo1);
        when(nestedTypeInfo1.isSimple()).thenReturn(true);

        when(nestedElement2.getName()).thenReturn("city");
        when(nestedElement2.getTypeInfo()).thenReturn(nestedTypeInfo2);
        when(nestedTypeInfo2.isSimple()).thenReturn(true);

        // Act
        List<String> result = extractor.extractPramNames(List.of(element));

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("street", result.get(0));
        assertEquals("city", result.get(1));
    }

    @Test
    @DisplayName("extractPramNames should handle wrapped types in nested elements")
    void testExtractPramNames_wrappedNestedTypes() {
        // Arrange
        ElementInfo element = mock(ElementInfo.class);
        TypeInfo complexTypeInfo = mock(TypeInfo.class);
        TypeMirror complexTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo nestedElement = mock(AttachedElementInfo.class);
        TypeInfo nestedTypeInfo = mock(TypeInfo.class);
        TypeMirror wrappedTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo deepNestedElement = mock(AttachedElementInfo.class);
        TypeInfo deepNestedTypeInfo = mock(TypeInfo.class);

        when(deepNestedTypeInfo.getRawType()).thenReturn(mock(Element.class));

        when(element.getTypeInfo()).thenReturn(complexTypeInfo);
        when(complexTypeInfo.isSimple()).thenReturn(false);
        when(complexTypeInfo.getMirror()).thenReturn(complexTypeMirror);

        when(cache.get(complexTypeMirror)).thenReturn(Optional.of(List.of(nestedElement)));

        when(nestedElement.getTypeInfo()).thenReturn(nestedTypeInfo);
        when(nestedTypeInfo.isSimple()).thenReturn(false);
        when(nestedTypeInfo.isWrapped()).thenReturn(true);
        when(nestedTypeInfo.getWrappedType()).thenReturn(wrappedTypeMirror);

        when(cache.get(wrappedTypeMirror)).thenReturn(Optional.of(List.of(deepNestedElement)));

        when(deepNestedElement.getName()).thenReturn("value");
        when(deepNestedElement.getTypeInfo()).thenReturn(deepNestedTypeInfo);
        when(deepNestedTypeInfo.isSimple()).thenReturn(true);

        // Act
        List<String> result = extractor.extractPramNames(List.of(element));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("value", result.get(0));
    }

    @Test
    @DisplayName("extractPramNames should handle non-wrapped nested complex types")
    void testExtractPramNames_nonWrappedNestedComplexTypes() {
        // Arrange
        ElementInfo element = mock(ElementInfo.class);
        TypeInfo complexTypeInfo = mock(TypeInfo.class);
        TypeMirror complexTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo nestedElement = mock(AttachedElementInfo.class);
        TypeInfo nestedTypeInfo = mock(TypeInfo.class);
        TypeMirror nestedTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo deepNestedElement = mock(AttachedElementInfo.class);
        TypeInfo deepNestedTypeInfo = mock(TypeInfo.class);

        when(deepNestedTypeInfo.getRawType()).thenReturn(mock(Element.class));

        when(element.getTypeInfo()).thenReturn(complexTypeInfo);
        when(complexTypeInfo.isSimple()).thenReturn(false);
        when(complexTypeInfo.getMirror()).thenReturn(complexTypeMirror);

        when(cache.get(complexTypeMirror)).thenReturn(Optional.of(List.of(nestedElement)));

//        when(nestedElement.getName()).thenReturn("nested");
        when(nestedElement.getTypeInfo()).thenReturn(nestedTypeInfo);
        when(nestedTypeInfo.isSimple()).thenReturn(false);
        when(nestedTypeInfo.isWrapped()).thenReturn(false);
        when(nestedTypeInfo.getMirror()).thenReturn(nestedTypeMirror);

        when(cache.get(nestedTypeMirror)).thenReturn(Optional.of(List.of(deepNestedElement)));

        when(deepNestedElement.getName()).thenReturn("deepValue");
        when(deepNestedElement.getTypeInfo()).thenReturn(deepNestedTypeInfo);
        when(deepNestedTypeInfo.isSimple()).thenReturn(true);

        // Act
        List<String> result = extractor.extractPramNames(List.of(element));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("deepValue", result.get(0));
    }

    @Test
    @DisplayName("extractPramNames should handle empty cache for nested types")
    void testExtractPramNames_emptyCacheForNestedTypes() {
        // Arrange
        ElementInfo element = mock(ElementInfo.class);
        TypeInfo complexTypeInfo = mock(TypeInfo.class);
        TypeMirror complexTypeMirror = mock(TypeMirror.class);

        when(element.getTypeInfo()).thenReturn(complexTypeInfo);
        when(complexTypeInfo.isSimple()).thenReturn(false);
        when(complexTypeInfo.getMirror()).thenReturn(complexTypeMirror);

        when(cache.get(complexTypeMirror)).thenReturn(Optional.empty());

        // Act
        List<String> result = extractor.extractPramNames(List.of(element));

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractPramNames should handle mix of simple and complex elements")
    void testExtractPramNames_mixedElements() {
        // Arrange
        ElementInfo simpleElement = new ElementInfo(typeInfo, "simpleParam");
        ElementInfo complexElement = mock(ElementInfo.class);
        TypeInfo complexTypeInfo = mock(TypeInfo.class);
        TypeMirror complexTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo nestedElement = mock(AttachedElementInfo.class);
        TypeInfo nestedTypeInfo = mock(TypeInfo.class);
        when(nestedTypeInfo.getRawType()).thenReturn(mock(Element.class));

        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.getRawType()).thenReturn(mock(Element.class));

        when(complexElement.getTypeInfo()).thenReturn(complexTypeInfo);
        when(complexTypeInfo.isSimple()).thenReturn(false);
        when(complexTypeInfo.getMirror()).thenReturn(complexTypeMirror);

        when(cache.get(complexTypeMirror)).thenReturn(Optional.of(List.of(nestedElement)));

        when(nestedElement.getName()).thenReturn("nestedParam");
        when(nestedElement.getTypeInfo()).thenReturn(nestedTypeInfo);
        when(nestedTypeInfo.isSimple()).thenReturn(true);

        // Act
        List<String> result = extractor.extractPramNames(List.of(simpleElement, complexElement));

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("simpleParam", result.get(0));
        assertEquals("nestedParam", result.get(1));
    }

    @Test
    @DisplayName("extractPramNames should handle deeply nested structures")
    void testExtractPramNames_deeplyNestedStructure() {
        // Arrange
        ElementInfo element = mock(ElementInfo.class);
        TypeInfo level1TypeInfo = mock(TypeInfo.class);
        TypeMirror level1TypeMirror = mock(TypeMirror.class);

        AttachedElementInfo level2Element = mock(AttachedElementInfo.class);
        TypeInfo level2TypeInfo = mock(TypeInfo.class);
        TypeMirror level2TypeMirror = mock(TypeMirror.class);

        AttachedElementInfo level3Element = mock(AttachedElementInfo.class);
        TypeInfo level3TypeInfo = mock(TypeInfo.class);

        when(element.getTypeInfo()).thenReturn(level1TypeInfo);
        when(level1TypeInfo.isSimple()).thenReturn(false);
        when(level1TypeInfo.getMirror()).thenReturn(level1TypeMirror);

        when(cache.get(level1TypeMirror)).thenReturn(Optional.of(List.of(level2Element)));

//        when(level2Element.getName()).thenReturn("level2");
        when(level2Element.getTypeInfo()).thenReturn(level2TypeInfo);
        when(level2TypeInfo.isSimple()).thenReturn(false);
        when(level2TypeInfo.isWrapped()).thenReturn(false);
        when(level2TypeInfo.getMirror()).thenReturn(level2TypeMirror);

        when(cache.get(level2TypeMirror)).thenReturn(Optional.of(List.of(level3Element)));

        when(level3Element.getName()).thenReturn("level3");
        when(level3Element.getTypeInfo()).thenReturn(level3TypeInfo);
        when(level3TypeInfo.isSimple()).thenReturn(true);
        when(level3TypeInfo.getRawType()).thenReturn(mock(Element.class));

        // Act
        List<String> result = extractor.extractPramNames(List.of(element));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("level3", result.get(0));
    }
}
