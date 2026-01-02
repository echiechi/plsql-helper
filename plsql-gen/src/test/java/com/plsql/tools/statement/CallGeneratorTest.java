package com.plsql.tools.statement;

import com.plsql.tools.statement.params.Parameter;
import com.plsql.tools.statement.params.ParameterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CallGeneratorTest {

    private TestCallGenerator generator;

    @BeforeEach
    public void setUp() {
        generator = new TestCallGenerator("test_pkg", "test_func");
    }

    @Test
    public void testConstructor() {
        assertEquals("test_pkg", generator.getPackageName());
        assertEquals("test_func", generator.getName());
        assertEquals(0, generator.getParametersSize());
        assertEquals("", generator.getSuffix());
    }

    @Test
    public void testConstructorWithEmptyPackage() {
        TestCallGenerator emptyPkgGenerator = new TestCallGenerator("", "test_func");
        assertEquals("", emptyPkgGenerator.getPackageName());
        assertEquals("test_func", emptyPkgGenerator.getName());
    }

    @Test
    public void testWithSuffix() {
        generator.withSuffix("_v1");
        assertEquals("_v1", generator.getSuffix());
    }

    @Test
    public void testWithSuffixNull() {
        generator.withSuffix(null);
        assertNull(generator.getSuffix());
    }

    @Test
    public void testWithParameter() {
        generator.withParameter("userId");
        assertEquals(1, generator.getParametersSize());
        
        Parameter param = generator.getParameter(0);
        assertEquals("user_id", param.getName());
        assertEquals(ParameterType.IN, param.getType());
    }

    @Test
    public void testWithParameterNullOrEmpty() {
        generator.withParameter(null);
        assertEquals(0, generator.getParametersSize());
        
        generator.withParameter("");
        assertEquals(0, generator.getParametersSize());
        
        generator.withParameter("   ");
        assertEquals(0, generator.getParametersSize());
    }

    @Test
    public void testWithMultipleParameters() {
        generator.withParameter("firstName");
        generator.withParameter("lastName");
        generator.withParameter("userAge");
        
        assertEquals(3, generator.getParametersSize());
        assertEquals("first_name", generator.getParameter(0).getName());
        assertEquals("last_name", generator.getParameter(1).getName());
        assertEquals("user_age", generator.getParameter(2).getName());
    }

    @Test
    public void testFormatFullName() {
        assertEquals("test_pkg_test_func", generator.formatFullName());
    }

    @Test
    public void testFormatFullNameWithEmptyPackage() {
        TestCallGenerator emptyPkgGenerator = new TestCallGenerator("", "test_func");
        assertEquals("test_func", emptyPkgGenerator.formatFullName());
    }

    @Test
    public void testFormatParameters() {
        generator.withParameter("param1");
        generator.withParameter("param2");
        
        String formatted = generator.formatParameters();
        assertEquals("param1 => ?,param2 => ?", formatted);
    }

    @Test
    public void testFormatParametersEmpty() {
        assertEquals("", generator.formatParameters());
    }

    @Test
    public void testFormatCallName() {
        assertEquals("test_pkg.", generator.formatCallName());
    }

    @Test
    public void testFormatCallNameWithEmptyPackage() {
        TestCallGenerator emptyPkgGenerator = new TestCallGenerator("", "test_func");
        assertEquals("", emptyPkgGenerator.formatCallName());
    }

    @Test
    public void testGenerate() {
        String result = generator.generate();
        assertEquals("TEST_TEMPLATE_OUTPUT", result);
    }

    // Concrete implementation for testing
    private static class TestCallGenerator extends CallGenerator {
        
        public TestCallGenerator(String packageName, String name) {
            super(packageName, name);
        }

        @Override
        public String buildWithTemplate() {
            return "TEST_TEMPLATE_OUTPUT";
        }

        // Expose protected methods for testing
        public String getPackageName() {
            return packageName;
        }

        public String getName() {
            return name;
        }

        public String getSuffix() {
            return suffix;
        }

        public int getParametersSize() {
            return parameters.size();
        }

        public Parameter getParameter(int index) {
            return parameters.get(index);
        }

        // Expose protected methods for testing
        public String formatFullName() {
            return super.formatFullName();
        }

        public String formatParameters() {
            return super.formatParameters();
        }

        public String formatCallName() {
            return super.formatCallName();
        }
    }
}