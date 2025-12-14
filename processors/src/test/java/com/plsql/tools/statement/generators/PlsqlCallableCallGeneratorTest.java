package com.plsql.tools.statement.generators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlsqlCallableCallGeneratorTest {

    private ProcedureCallGenerator generator;

    @BeforeEach
    public void setUp() {
        generator = new ProcedureCallGenerator("user_pkg", "insert_user");
    }

    @Test
    public void testConstructor() {
        assertNotNull(generator);
    }

    @Test
    public void testBuild() {
        String buildResult = generator.build();
        String templateResult = generator.buildWithTemplate();
        
        assertEquals(templateResult, buildResult);
    }

    @Test
    public void testBuildWithTemplateBasic() {
        String result = generator.buildWithTemplate();
        
        assertTrue(result.contains("public static final String user_pkg_insert_user"));
        assertTrue(result.contains("{ call user_pkg.insert_user() }"));
        assertFalse(result.contains("? =")); // Procedures don't have return values
    }

    @Test
    public void testBuildWithTemplateWithParameters() {
        generator.withParameter("userId");
        generator.withParameter("userName");
        
        String result = generator.buildWithTemplate();
        
        assertTrue(result.contains("user_pkg_insert_user"));
        assertTrue(result.contains("user_id => ?,user_name => ?"));
        assertTrue(result.contains("{ call user_pkg.insert_user(user_id => ?,user_name => ?) }"));
    }

    @Test
    public void testBuildWithTemplateWithSuffix() {
        generator.withSuffix("_BATCH");
        
        String result = generator.buildWithTemplate();
        
        assertTrue(result.contains("user_pkg_insert_user_BATCH"));
        assertTrue(result.contains("{ call user_pkg.insert_user() }"));
    }

    @Test
    public void testBuildWithTemplateEmptyPackage() {
        ProcedureCallGenerator emptyPkgGenerator = new ProcedureCallGenerator("", "insert_user");
        
        String result = emptyPkgGenerator.buildWithTemplate();
        
        assertTrue(result.contains("insert_user"));
        assertTrue(result.contains("{ call insert_user() }"));
    }

    @Test
    public void testBuildWithTemplateComplexScenario() {
        generator.withParameter("firstName");
        generator.withParameter("lastName");
        generator.withParameter("userEmail");
        generator.withSuffix("_V3");
        
        String result = generator.buildWithTemplate();
        
        assertTrue(result.contains("user_pkg_insert_user_V3"));
        assertTrue(result.contains("first_name => ?,last_name => ?,user_email => ?"));
        assertTrue(result.contains("{ call user_pkg.insert_user(first_name => ?,last_name => ?,user_email => ?) }"));
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
        assertTrue(result.contains("{ call"));
        assertTrue(result.endsWith("}\";\r\n"));
        assertFalse(result.contains("? =")); // Key difference from functions
    }

    @Test
    public void testDifferenceFromFunction() {
        FunctionCallGenerator functionGen = new FunctionCallGenerator("user_pkg", "get_user");
        String functionResult = functionGen.buildWithTemplate();
        String procedureResult = generator.buildWithTemplate();
        
        assertTrue(functionResult.contains("? = call"));
        assertTrue(procedureResult.contains("{ call"));
        assertFalse(procedureResult.contains("? = call"));
    }
}