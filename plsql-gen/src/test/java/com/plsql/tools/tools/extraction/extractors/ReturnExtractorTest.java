package com.plsql.tools.tools.extraction.extractors;

import com.plsql.tools.annotations.Output;
import com.plsql.tools.annotations.PlsqlCallable;
import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.cache.SimpleCache;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReturnExtractor Tests")
class ReturnExtractorTest {

    @Mock
    private TypeInfoExtractor typeInfoExtractor;

    @Mock
    private ComposedElementExtractor composedElementExtractor;

    @Mock
    private SimpleCache<TypeMirror, List<AttachedElementInfo>> cache;

    @Mock
    private ExecutableElement method;

    @Mock
    private DeclaredType returnType;

    @Mock
    private TypeMirror typeMirror;

    @Mock
    private Element element;

    @Mock
    private PlsqlCallable plsqlCallable;

    @Mock
    private Output output;

    private ReturnExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ReturnExtractor(typeInfoExtractor, composedElementExtractor, cache);
    }

    @Test
    @DisplayName("extractReturn should return empty list for void return type")
    void testExtractReturn_voidReturnType() {
        // Arrange
        TypeMirror voidType = mock(TypeMirror.class);
        when(method.getReturnType()).thenReturn(voidType);
        when(voidType.toString()).thenReturn("void");

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractReturn should throw exception when return type exists but no @Output annotation")
    void testExtractReturn_noOutputAnnotation() {
        // Arrange
        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[0]);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> extractor.extractReturn(method)
        );

        assertTrue(exception.getMessage().contains("Method has a return type but no @Output annotation"));
    }

    @Test
    @DisplayName("extractReturn should extract single output with simple type")
    void testExtractReturn_singleOutputSimpleType() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);
        Element returnElement = mock(Element.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("");
//        when(output.value()).thenReturn("p_result");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
//       when(typeInfo.typeAsString()).thenReturn("java.lang.String");
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        ReturnElementInfo returnInfo = result.get(0);
        assertNotNull(returnInfo);
        assertEquals(typeInfo, returnInfo.getTypeInfo());
    }

    @Test
    @DisplayName("extractReturn should throw exception for simple type that cannot be mapped to JDBC")
    void testExtractReturn_simpleTypeCannotBeMapped() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("UnknownType");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.typeAsString()).thenReturn("UnknownType");
        when(typeInfo.asTypeMapper()).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> extractor.extractReturn(method)
        );

        assertTrue(exception.getMessage().contains("Type cannot be mapped to JDBC type"));
    }

    @Test
    @DisplayName("extractReturn should handle wrapped return type")
    void testExtractReturn_wrappedReturnType() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);
        Element wrappedElement = mock(Element.class);
        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("List<User>");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getRawWrappedType()).thenReturn(wrappedElement);

        when(composedElementExtractor.convertInto(wrappedElement)).thenReturn(composedInfo);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(composedElementExtractor).convertInto(wrappedElement);
        verify(composedInfo).setTypeInfo(typeInfo);
    }

    @Test
    @DisplayName("extractReturn should handle composed return type")
    void testExtractReturn_composedReturnType() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);
        Element returnElement = mock(Element.class);
        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("User");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(false);
        when(typeInfo.getRawType()).thenReturn(returnElement);

        when(composedElementExtractor.convertInto(returnElement, typeInfo)).thenReturn(composedInfo);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(composedElementExtractor).convertInto(returnElement, typeInfo);
    }

    @Test
    @DisplayName("extractReturn should extract multiple outputs")
    void testExtractReturn_multipleOutputs() {
        // Arrange
        Output output1 = mock(Output.class);
        Output output2 = mock(Output.class);

        AttachedElementInfo attachedElement1 = mock(AttachedElementInfo.class);
        AttachedElementInfo attachedElement2 = mock(AttachedElementInfo.class);

        TypeInfo typeInfo1 = mock(TypeInfo.class);
        TypeInfo typeInfo2 = mock(TypeInfo.class);

        Element returnElement = mock(Element.class);
        TypeMirror returnTypeMirror = mock(TypeMirror.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("UserResult");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output1, output2});

        when(returnType.asElement()).thenReturn(returnElement);
        when(returnElement.asType()).thenReturn(returnTypeMirror);

        when(output1.field()).thenReturn("userId");
        when(output2.field()).thenReturn("userName");

        when(cache.get(returnTypeMirror)).thenReturn(Optional.of(List.of(attachedElement1, attachedElement2)));

        when(attachedElement1.getName()).thenReturn("userId");
        when(attachedElement1.getTypeInfo()).thenReturn(typeInfo1);
        when(typeInfo1.isSimple()).thenReturn(true);
        when(typeInfo1.asTypeMapper()).thenReturn(TypeMapper.LONG);

        when(attachedElement2.getName()).thenReturn("userName");
        when(attachedElement2.getTypeInfo()).thenReturn(typeInfo2);
        when(typeInfo2.isSimple()).thenReturn(true);
        when(typeInfo2.asTypeMapper()).thenReturn(TypeMapper.STRING);

        TypeInfo returnTypeInfo = mock(TypeInfo.class);
        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(returnTypeInfo);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(cache).get(returnTypeMirror);
    }

    @Test
    @DisplayName("extractReturn should throw exception when return type not in cache for multiple outputs")
    void testExtractReturn_multipleOutputsNotInCache() {
        // Arrange
        Output output1 = mock(Output.class);
        Output output2 = mock(Output.class);

        Element returnElement = mock(Element.class);
        TypeMirror returnTypeMirror = mock(TypeMirror.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("UserResult");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output1, output2});

        when(returnType.asElement()).thenReturn(returnElement);
        when(returnElement.asType()).thenReturn(returnTypeMirror);

        when(cache.get(returnTypeMirror)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> extractor.extractReturn(method)
        );

        assertTrue(exception.getMessage().contains("Return type not in cache"));
    }

    @Test
    @DisplayName("extractReturn should handle output with non-matching field name")
    void testExtractReturn_multipleOutputsNonMatchingField() {
        // Arrange
        Output output1 = mock(Output.class);
        Output output2 = mock(Output.class);

        Element returnElement = mock(Element.class);
        TypeMirror returnTypeMirror = mock(TypeMirror.class);

        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("UserResult");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output1, output2});

        when(returnType.asElement()).thenReturn(returnElement);
        when(returnElement.asType()).thenReturn(returnTypeMirror);

        when(output1.field()).thenReturn("nonExistentField");
        when(output2.field()).thenReturn("anotherNonExistentField");

        when(cache.get(returnTypeMirror)).thenReturn(Optional.of(List.of(attachedElement)));

        when(attachedElement.getName()).thenReturn("userId");

        TypeInfo returnTypeInfo = mock(TypeInfo.class);
        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(returnTypeInfo);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractReturn should use default name when field is blank in single output")
    void testExtractReturn_singleOutputBlankField() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("extractReturn should use custom field name when provided")
    void testExtractReturn_customFieldName() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("customName");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("customName", result.get(0).getName());
    }

    @Test
    @DisplayName("extractReturn should set parent for multiple outputs")
    void testExtractReturn_multipleOutputsWithParent() {
        // Arrange
        Output output1 = mock(Output.class);
        Output output2 = mock(Output.class);

        AttachedElementInfo attachedElement1 = mock(AttachedElementInfo.class);
        AttachedElementInfo attachedElement2 = mock(AttachedElementInfo.class);
        TypeInfo attachedTypeInfo1 = mock(TypeInfo.class);
        TypeInfo attachedTypeInfo2 = mock(TypeInfo.class);

        Element returnElement = mock(Element.class);
        TypeMirror returnTypeMirror = mock(TypeMirror.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("UserResult");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output1, output2});

        when(returnType.asElement()).thenReturn(returnElement);
        when(returnElement.asType()).thenReturn(returnTypeMirror);

        when(output1.field()).thenReturn("userId");
        when(output2.field()).thenReturn("userName");

        when(cache.get(returnTypeMirror)).thenReturn(Optional.of(List.of(attachedElement1, attachedElement2)));

        when(attachedElement1.getName()).thenReturn("userId");
        when(attachedElement1.getTypeInfo()).thenReturn(attachedTypeInfo1);
        when(attachedTypeInfo1.isSimple()).thenReturn(true);
        when(attachedTypeInfo1.asTypeMapper()).thenReturn(TypeMapper.LONG);

        when(attachedElement2.getName()).thenReturn("userName");
        when(attachedElement2.getTypeInfo()).thenReturn(attachedTypeInfo2);
        when(attachedTypeInfo2.isSimple()).thenReturn(true);
        when(attachedTypeInfo2.asTypeMapper()).thenReturn(TypeMapper.STRING);

        TypeInfo returnTypeInfo = mock(TypeInfo.class);
        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(returnTypeInfo);

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).hasParent());
        assertNotNull(result.get(0).getParent());
        assertTrue(result.get(1).hasParent());
        assertNotNull(result.get(1).getParent());
    }

    @Test
    @DisplayName("extractReturn should handle complex wrapped type")
    void testExtractReturn_complexWrappedType() {
        // Arrange
        TypeInfo typeInfo = mock(TypeInfo.class);
        Element wrappedElement = mock(Element.class);
        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);

        when(method.getReturnType()).thenReturn(returnType);
        when(returnType.toString()).thenReturn("Optional<User>");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(new Output[]{output});

        when(output.field()).thenReturn("user");

        when(typeInfoExtractor.extractTypeInfo(returnType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getRawWrappedType()).thenReturn(wrappedElement);

        when(composedElementExtractor.convertInto(wrappedElement)).thenReturn(composedInfo);
        when(composedInfo.getName()).thenReturn("user");

        // Act
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user", result.get(0).getName());
        verify(composedInfo).setName("user");
    }
}
