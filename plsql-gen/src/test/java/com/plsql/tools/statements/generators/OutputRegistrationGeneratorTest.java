package com.plsql.tools.statements.generators;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.plsql.tools.tools.CodeGenConstants.POSITION_VAR;
import static com.plsql.tools.tools.CodeGenConstants.STATEMENT_VAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutputRegistrationGeneratorTest {
    @Test
    public void testConstructorWithNull() {
        OutputRegistrationGenerator generator = new OutputRegistrationGenerator(null);
        assertNotNull(generator);
    }

    @Test
    public void testConstructorNonNull() {
        OutputRegistrationGenerator generator = new OutputRegistrationGenerator(List.of());
        assertNotNull(generator);
    }

    @Test
    public void testGenerationWithNullOrEmpty() {
        OutputRegistrationGenerator generatorOfNull = new OutputRegistrationGenerator(null);
        var generatedNull = generatorOfNull.generate();

        OutputRegistrationGenerator generatorOfEmpty = new OutputRegistrationGenerator(List.of());
        var generatedOfEmpty = generatorOfEmpty.generate();

        assertEquals(generatedNull, "");
        assertEquals(generatedOfEmpty, "");
    }

    @Test
    public void testGenerationWithOneOutput() {
        var returnElement = mock(ReturnElementInfo.class);
        var typeInfo = mock(TypeInfo.class);

        when(returnElement.getTypeInfo()).thenReturn(typeInfo);
        when(typeInfo.isWrapped()).thenReturn(false);

        OutputRegistrationGenerator generator = new OutputRegistrationGenerator(List.of(returnElement));
        var generated = generator.generate();
        assertEquals(generated, "%s.registerOutParameter(%s, JDBCType.REF_CURSOR);\n".formatted(STATEMENT_VAR, POSITION_VAR));
    }

    @Test
    public void testGenerationWithOneOutputSpecificJDBCType() {
        for (var type : TypeMapper.values()) {
            var returnElement = mock(ReturnElementInfo.class);
            var typeInfo = mock(TypeInfo.class);

            when(returnElement.getTypeInfo()).thenReturn(typeInfo);
            when(typeInfo.isWrapped()).thenReturn(false);
            when(typeInfo.asTypeMapper()).thenReturn(type);
            OutputRegistrationGenerator generator = new OutputRegistrationGenerator(List.of(returnElement));
            var generated = generator.generate();

            assertEquals(generated, "%s.registerOutParameter(%s, JDBCType.%s);\n".formatted(
                    STATEMENT_VAR,
                    POSITION_VAR,
                    type.getJdbcType().name()));
        }
    }

    @Test
    public void testGenerationWithOneOutputWrappedSpecificJDBCType() {
        for (var type : TypeMapper.values()) {
            var returnElement = mock(ReturnElementInfo.class);
            var typeInfo = mock(TypeInfo.class);

            when(returnElement.getTypeInfo()).thenReturn(typeInfo);
            when(typeInfo.isWrapped()).thenReturn(true);
            when(typeInfo.wrappedTypeAsTypeMapper()).thenReturn(type);

            OutputRegistrationGenerator generator = new OutputRegistrationGenerator(List.of(returnElement));
            var generated = generator.generate();

            assertEquals(generated, "%s.registerOutParameter(%s, JDBCType.%s);\n".formatted(
                    STATEMENT_VAR,
                    POSITION_VAR,
                    type.getJdbcType().name()));
        }
    }

    @Test
    public void testGenerationWithMultipleElements() {
        for (var type : TypeMapper.values()) {
            var returnElement1 = mock(ReturnElementInfo.class);
            var typeInfo1 = mock(TypeInfo.class);

            when(returnElement1.getTypeInfo()).thenReturn(typeInfo1);
            when(typeInfo1.isWrapped()).thenReturn(true);
            when(typeInfo1.wrappedTypeAsTypeMapper()).thenReturn(type);

            var returnElement2 = mock(ReturnElementInfo.class);
            var typeInfo2 = mock(TypeInfo.class);
            when(returnElement2.getTypeInfo()).thenReturn(typeInfo2);
            when(typeInfo2.isWrapped()).thenReturn(false);
            when(typeInfo2.asTypeMapper()).thenReturn(type);

            OutputRegistrationGenerator generator = new OutputRegistrationGenerator(
                    List.of(returnElement1, returnElement2));

            var generated = generator.generate();

            assertEquals(generated,
                    "%s.registerOutParameter(%s++, JDBCType.%s);\n%s.registerOutParameter(%s, JDBCType.%s);\n".formatted(
                            STATEMENT_VAR,
                            POSITION_VAR,
                            type.getJdbcType().name(),
                            STATEMENT_VAR,
                            POSITION_VAR,
                            type.getJdbcType().name()));
        }
    }

}
