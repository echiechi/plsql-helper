package com.plsql.tools.statements.generators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionCallGeneratorTest {

    private FunctionCallGenerator generator;

    @BeforeEach
    public void setUp() {
        generator = new FunctionCallGenerator("user_pkg", "get_user");
    }

    @Test
    public void testConstructor() {
        assertNotNull(generator);
        assertEquals("user_pkg_get_user", generator.generate().split(" ")[4]);
    }

    @Test
    public void testBuildWithTemplateBasic() {
        String result = generator.buildWithTemplate();

        assertTrue(result.contains("public static final String user_pkg_get_user"));
        assertTrue(result.contains("{ ? = call user_pkg.get_user() }"));
    }

    @Test
    public void testBuildWithTemplateWithParameters() {
        generator.withParameter("userId");
        generator.withParameter("userName");

        String result = generator.buildWithTemplate();

        assertTrue(result.contains("user_pkg_get_user"));
        assertTrue(result.contains("user_id => ?,user_name => ?"));
        assertTrue(result.contains("{ ? = call user_pkg.get_user(user_id => ?,user_name => ?) }"));
    }

    @Test
    public void testBuildWithTemplateWithSuffix() {
        generator.withSuffix("_V2");

        String result = generator.buildWithTemplate();

        assertTrue(result.contains("user_pkg_get_user_V2"));
        assertTrue(result.contains("{ ? = call user_pkg.get_user() }"));
    }

    @Test
    public void testBuildWithTemplateEmptyPackage() {
        FunctionCallGenerator emptyPkgGenerator = new FunctionCallGenerator("", "get_user");

        String result = emptyPkgGenerator.buildWithTemplate();

        assertTrue(result.contains("get_user"));
        assertTrue(result.contains("{ ? = call get_user() }"));
    }

    @Test
    public void testBuildWithTemplateComplexScenario() {
        generator.withParameter("firstName");
        generator.withParameter("lastName");
        generator.withSuffix("_LATEST");

        String result = generator.buildWithTemplate();

        assertTrue(result.contains("user_pkg_get_user_LATEST"));
        assertTrue(result.contains("first_name => ?,last_name => ?"));
        assertTrue(result.contains("{ ? = call user_pkg.get_user(first_name => ?,last_name => ?) }"));
    }

    @Test
    public void testGenerateCallsBuilWithTemplate() {
        String buildResult = generator.buildWithTemplate();
        String generateResult = generator.generate();

        assertEquals(buildResult, generateResult);
    }

    @Test
    public void testTemplateStructure() {
        String result = generator.buildWithTemplate();

        assertTrue(result.startsWith("public static final String"));
        assertTrue(result.contains("{ ? = call"));
        assertTrue(result.endsWith("}\";\r\n"));
        assertTrue(result.endsWith("\n"));

    }
}