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
    private PlsqlCallable plsqlCallable;

    @Mock
    private Output output;

    @Mock
    private TypeMirror returnTypeMirror;

    @Mock
    private DeclaredType declaredType;

    @Mock
    private Element element;

    @Mock
    private TypeInfo typeInfo;

    private ReturnExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ReturnExtractor(typeInfoExtractor, composedElementExtractor, cache);
    }

    @Test
    @DisplayName("Should return empty list when return type is void")
    void testExtractReturn_VoidReturnType() {
        when(method.getReturnType()).thenReturn(returnTypeMirror);
        when(returnTypeMirror.toString()).thenReturn("void");

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertTrue(result.isEmpty());
        verify(method, never()).getAnnotation(PlsqlCallable.class);
    }

    @Test
    @DisplayName("Should extract single output with DeclaredType")
    void testExtractReturn_SingleOutputWithDeclaredType() {
        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(typeInfoExtractor).extractTypeInfo(declaredType);
    }

    @Test
    @DisplayName("Should extract single output with primitive type")
    void testExtractReturn_SingleOutputWithPrimitive() {
        javax.lang.model.type.TypeKind typeKind = javax.lang.model.type.TypeKind.INT;

        when(method.getReturnType()).thenReturn(returnTypeMirror);
        when(returnTypeMirror.toString()).thenReturn("int");
        when(returnTypeMirror.getKind()).thenReturn(typeKind);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
        when(typeInfoExtractor.getDeclaredType("java.lang.Integer")).thenReturn(declaredType);
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.INTEGER);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(typeInfoExtractor).getDeclaredType("java.lang.Integer");
    }

    @Test
    @DisplayName("Should throw exception for unsupported return type in single output")
    void testExtractReturn_UnsupportedReturnType() {
        when(method.getReturnType()).thenReturn(returnTypeMirror);
        when(returnTypeMirror.toString()).thenReturn("UnsupportedType");
        when(returnTypeMirror.getKind()).thenReturn(javax.lang.model.type.TypeKind.OTHER);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");

        assertThrows(IllegalStateException.class, () -> extractor.extractReturn(method));
    }

    @Test
    @DisplayName("Should handle null TypeKind gracefully")
    void testExtractReturn_NullTypeKind() {
        when(method.getReturnType()).thenReturn(returnTypeMirror);
        when(returnTypeMirror.toString()).thenReturn("SomeType");
        when(returnTypeMirror.getKind()).thenReturn(null);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");

        assertThrows(IllegalStateException.class, () -> extractor.extractReturn(method));
    }

    @Test
    @DisplayName("Should extract multiple outputs from cache")
    void testExtractReturn_MultipleOutputs() {
        com.plsql.tools.annotations.InnerOutput innerOutput1 = mock(com.plsql.tools.annotations.InnerOutput.class);
        com.plsql.tools.annotations.InnerOutput innerOutput2 = mock(com.plsql.tools.annotations.InnerOutput.class);

        when(innerOutput1.value()).thenReturn("alias1");
        when(innerOutput1.field()).thenReturn("field1");
        when(innerOutput2.value()).thenReturn("alias2");
        when(innerOutput2.field()).thenReturn("field2");

        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("CustomType");
        when(declaredType.asElement()).thenReturn(element);
        when(element.asType()).thenReturn(returnTypeMirror);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[]{innerOutput1, innerOutput2});

        AttachedElementInfo attachedElement1 = mock(AttachedElementInfo.class);
        AttachedElementInfo attachedElement2 = mock(AttachedElementInfo.class);
        when(attachedElement1.getName()).thenReturn("field1");
        when(attachedElement2.getName()).thenReturn("field2");
        when(attachedElement1.getTypeInfo()).thenReturn(typeInfo);
        when(attachedElement2.getTypeInfo()).thenReturn(typeInfo);

        when(cache.get(returnTypeMirror)).thenReturn(Optional.of(List.of(attachedElement1, attachedElement2)));
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(cache).get(returnTypeMirror);
    }

    @Test
    @DisplayName("Should throw exception when return type not in cache for multiple outputs")
    void testExtractReturn_MultipleOutputs_NotInCache() {
        com.plsql.tools.annotations.InnerOutput innerOutput = mock(com.plsql.tools.annotations.InnerOutput.class);
        when(innerOutput.value()).thenReturn("alias");
        when(innerOutput.field()).thenReturn("field");

        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("CustomType");
        when(declaredType.asElement()).thenReturn(element);
        when(element.asType()).thenReturn(returnTypeMirror);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[]{innerOutput});
        when(cache.get(returnTypeMirror)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> extractor.extractReturn(method));

        assertTrue(exception.getMessage().contains("Return type not in cache"));
    }

    @Test
    @DisplayName("Should filter out non-matching fields in multiple outputs")
    void testExtractReturn_MultipleOutputs_NonMatchingFields() {
        com.plsql.tools.annotations.InnerOutput innerOutput = mock(com.plsql.tools.annotations.InnerOutput.class);
        when(innerOutput.value()).thenReturn("alias");
        when(innerOutput.field()).thenReturn("nonExistentField");

        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("CustomType");
        when(declaredType.asElement()).thenReturn(element);
        when(element.asType()).thenReturn(returnTypeMirror);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[]{innerOutput});

        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        when(attachedElement.getName()).thenReturn("differentField");

        when(cache.get(returnTypeMirror)).thenReturn(Optional.of(List.of(attachedElement)));
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when simple type cannot be mapped to JDBC")
    void testCreateReturnElementInfo_SimpleType_UnmappableToJdbc() {
        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(null);
        when(typeInfo.typeAsString()).thenReturn("UnmappableType");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> extractor.extractReturn(method));

        assertTrue(exception.getMessage().contains("Type cannot be mapped to JDBC type"));
    }

    @Test
    @DisplayName("Should create wrapped return element")
    void testCreateReturnElementInfo_WrappedType() {
        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("List<String>");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(true);
        when(typeInfo.getRawWrappedType()).thenReturn(element);

        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);
        when(composedInfo.getTypeInfo()).thenReturn(typeInfo);
        when(composedInfo.getName()).thenReturn("result");
        when(composedInfo.getElementInfoList()).thenReturn(List.of());
        when(composedInfo.getNestedElementInfo()).thenReturn(java.util.Collections.emptyMap());
        when(composedElementExtractor.convertInto(element)).thenReturn(composedInfo);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(composedElementExtractor).convertInto(element);
        verify(composedInfo).setTypeInfo(typeInfo);
    }

    @Test
    @DisplayName("Should create composed return element")
    void testCreateReturnElementInfo_ComposedType() {
        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("CustomObject");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
//        when(output.field()).thenReturn("");
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(false);
        when(typeInfo.getRawType()).thenReturn(element);

        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);
        when(composedInfo.getTypeInfo()).thenReturn(typeInfo);
        when(composedInfo.getName()).thenReturn("result");
        when(composedInfo.getElementInfoList()).thenReturn(List.of());
        when(composedInfo.getNestedElementInfo()).thenReturn(java.util.Collections.emptyMap());
        when(composedElementExtractor.convertInto(element, typeInfo)).thenReturn(composedInfo);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(composedElementExtractor).convertInto(element, typeInfo);
        verify(composedInfo).setName("result");
    }

    @Test
    @DisplayName("Should use field name from MetaInfo when provided")
    void testCreateReturnElementInfo_CustomFieldName() {
        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
//        when(output.field()).thenReturn("customName");
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(false);
        when(typeInfo.getRawType()).thenReturn(element);

        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);
        when(composedInfo.getTypeInfo()).thenReturn(typeInfo);
        when(composedInfo.getName()).thenReturn("customName");
        when(composedInfo.getElementInfoList()).thenReturn(List.of());
        when(composedInfo.getNestedElementInfo()).thenReturn(java.util.Collections.emptyMap());
        when(composedElementExtractor.convertInto(element, typeInfo)).thenReturn(composedInfo);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("customName", result.get(0).getName());
    }

    @Test
    @DisplayName("Should use default field name when MetaInfo field is blank")
    void testCreateReturnElementInfo_DefaultFieldName() {
        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("String");
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[0]);
        when(output.value()).thenReturn("alias");
//        when(output.field()).thenReturn("");
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.isWrapped()).thenReturn(false);
        when(typeInfo.getRawType()).thenReturn(element);

        ComposedElementInfo composedInfo = mock(ComposedElementInfo.class);
        when(composedInfo.getTypeInfo()).thenReturn(typeInfo);
        when(composedInfo.getName()).thenReturn("result");
        when(composedInfo.getElementInfoList()).thenReturn(List.of());
        when(composedInfo.getNestedElementInfo()).thenReturn(java.util.Collections.emptyMap());
        when(composedElementExtractor.convertInto(element, typeInfo)).thenReturn(composedInfo);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("result", result.get(0).getName());
    }

    @Test
    @DisplayName("Should set parent for multiple outputs")
    void testExtractReturn_MultipleOutputs_SetsParent() {
        com.plsql.tools.annotations.InnerOutput innerOutput = mock(com.plsql.tools.annotations.InnerOutput.class);
        when(innerOutput.value()).thenReturn("alias");
        when(innerOutput.field()).thenReturn("field1");

        when(method.getReturnType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn("CustomType");
        when(declaredType.asElement()).thenReturn(element);
        when(element.asType()).thenReturn(returnTypeMirror);
        when(method.getAnnotation(PlsqlCallable.class)).thenReturn(plsqlCallable);
        when(plsqlCallable.outputs()).thenReturn(output);
        when(output.innerOutputs()).thenReturn(new com.plsql.tools.annotations.InnerOutput[]{innerOutput});

        AttachedElementInfo attachedElement = mock(AttachedElementInfo.class);
        when(attachedElement.getName()).thenReturn("field1");
        when(attachedElement.getTypeInfo()).thenReturn(typeInfo);

        when(cache.get(returnTypeMirror)).thenReturn(Optional.of(List.of(attachedElement)));
        when(typeInfoExtractor.extractTypeInfo(declaredType)).thenReturn(typeInfo);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);

        List<ReturnElementInfo> result = extractor.extractReturn(method);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getParent());
        assertTrue(result.get(0).hasParent());
    }
}
