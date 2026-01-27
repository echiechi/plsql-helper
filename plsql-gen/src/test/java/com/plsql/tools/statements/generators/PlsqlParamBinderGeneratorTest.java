package com.plsql.tools.statements.generators;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.tools.extraction.info.AttachedElementInfo;
import com.plsql.tools.tools.extraction.info.ComposedElementInfo;
import com.plsql.tools.tools.extraction.info.ElementInfo;
import com.plsql.tools.tools.extraction.info.TypeInfo;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.plsql.tools.tools.CodeGenConstants.POSITION_VAR;
import static com.plsql.tools.tools.CodeGenConstants.STATEMENT_VAR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlsqlParamBinderGeneratorTest {

    @Test
    void shouldGenerateBindingForSimpleParameter() {
        // Test String parameter
        ElementInfo stringParam = createSimpleParameter("username", TypeMapper.STRING);
        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(stringParam), false);

        String result = generator.generate();

        assertEquals("stmt.setString(pos++,username);", result);
    }

    @Test
    void shouldGenerateBindingForMultipleSimpleParameters() {
        ElementInfo stringParam = createSimpleParameter("username", TypeMapper.STRING);
        ElementInfo intParam = createSimpleParameter("age", TypeMapper.INTEGER);
        ElementInfo boolParam = createSimpleParameter("active", TypeMapper.BOOLEAN);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(stringParam, intParam, boolParam), false);

        String result = generator.generate();

        String expected = "stmt.setString(pos++,username);\n" +
                "stmt.setInt(pos++,age);\n" +
                "stmt.setBoolean(pos++,active);";
        assertEquals(expected, result);
    }

    @Test
    void shouldTransformDateParameters() {
        // Test all date/time types
        ElementInfo dateParam = createSimpleParameter("birthDate", TypeMapper.DATE);
        ElementInfo localDateParam = createSimpleParameter("startDate", TypeMapper.LOCAL_DATE);
        ElementInfo localDateTimeParam = createSimpleParameter("timestamp", TypeMapper.LOCAL_DATE_TIME);
        ElementInfo localTimeParam = createSimpleParameter("timeOfDay", TypeMapper.LOCAL_TIME);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(dateParam, localDateParam, localDateTimeParam, localTimeParam), false);

        String result = generator.generate();

        String expected = "stmt.setDate(pos++,DateTools.toSqlDate(birthDate));\n" +
                "stmt.setDate(pos++,DateTools.toSqlDate(startDate));\n" +
                "stmt.setTimestamp(pos++,DateTools.toTimestamp(timestamp));\n" +
                "stmt.setTime(pos++,DateTools.toTime(timeOfDay));";
        assertEquals(expected, result);
    }

    @Test
    void shouldHandleCharacterToString() {
        // Test primitive char
        ElementInfo charParam = createSimpleParameter("initial", TypeMapper.CHARACTER);
        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(charParam), false);

        String result = generator.generate();

        assertEquals("stmt.setString(pos++,StringTools.toString(initial));", result);

        // Test Character wrapper
        ElementInfo charWrapperParam = createSimpleParameter("middleInitial", TypeMapper.CHARACTER_WRAPPER);
        PlsqlParamBinderGenerator generatorWrapper = new PlsqlParamBinderGenerator(List.of(charWrapperParam), false);

        String resultWrapper = generatorWrapper.generate();

        assertEquals("stmt.setString(pos++,StringTools.toString(middleInitial));", resultWrapper);
    }

    @Test
    void shouldFlattenNestedComposedParameters() {
        // Create a composed parameter with nested structure
        ComposedElementInfo composedParam = createComposedParameter("address", "Address");

        // Add simple fields to the composed parameter
        AttachedElementInfo streetField = createAttachedElement("street", TypeMapper.STRING, "getStreet");
        AttachedElementInfo cityField = createAttachedElement("city", TypeMapper.STRING, "getCity");

        composedParam.addElement(streetField);
        composedParam.addElement(cityField);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(composedParam), false);

        String result = generator.generate();

        String expected = "stmt.setString(pos++,address.getStreet());\n" +
                "stmt.setString(pos++,address.getCity());";
        assertEquals(expected, result);
    }

    @Test
    void shouldUsePreIncrementWhenConfigured() {
        ElementInfo stringParam = createSimpleParameter("username", TypeMapper.STRING);
        ElementInfo intParam = createSimpleParameter("age", TypeMapper.INTEGER);

        // Test with pre-increment enabled
        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(stringParam, intParam), true);

        String result = generator.generate();

        String expected = "stmt.setString(++pos,username);\n" +
                "stmt.setInt(++pos,age);";
        assertEquals(expected, result);
    }

    @Test
    void shouldUseCustomStatementName() {
        // NOTE: The generator uses constants (STATEMENT_VAR = "stmt" and POSITION_VAR = "pos")
        // This test verifies that the constants are used correctly
        ElementInfo param = createSimpleParameter("value", TypeMapper.STRING);
        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(param), false);

        String result = generator.generate();

        assertTrue(result.contains(STATEMENT_VAR + "."));
        assertTrue(result.contains(POSITION_VAR));
    }

    @Test
    void shouldGenerateCompleteBindingForComposedType() {
        // Create a Person object with multiple fields including different types
        ComposedElementInfo personParam = createComposedParameter("person", "Person");

        AttachedElementInfo nameField = createAttachedElement("name", TypeMapper.STRING, "getName");
        AttachedElementInfo ageField = createAttachedElement("age", TypeMapper.INTEGER, "getAge");
        AttachedElementInfo birthDateField = createAttachedElement("birthDate", TypeMapper.LOCAL_DATE, "getBirthDate");
        AttachedElementInfo activeField = createAttachedElement("active", TypeMapper.BOOLEAN, "isActive");

        personParam.addElement(nameField);
        personParam.addElement(ageField);
        personParam.addElement(birthDateField);
        personParam.addElement(activeField);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(personParam), false);

        String result = generator.generate();

        String expected = "stmt.setString(pos++,person.getName());\n" +
                "stmt.setInt(pos++,person.getAge());\n" +
                "stmt.setDate(pos++,DateTools.toSqlDate(person.getBirthDate()));\n" +
                "stmt.setBoolean(pos++,person.isActive());";
        assertEquals(expected, result);
    }

    @Test
    void shouldHandleDeeplyNestedStructures() {
        // Create Person -> Address -> City structure
        ComposedElementInfo personParam = createComposedParameter("person", "Person");

        // Person has a name (simple) and address (nested)
        AttachedElementInfo nameField = createAttachedElement("name", TypeMapper.STRING, "getName");
        personParam.addElement(nameField);

        // Create nested Address structure
        TypeMirror addressTypeMirror = mock(TypeMirror.class);
        when(addressTypeMirror.toString()).thenReturn("Address");

        AttachedElementInfo addressField = createNestedAttachedElement("address", addressTypeMirror, "getAddress");
        personParam.addElement(addressField);

        // Add nested elements for Address (street and nested city)
        Map<TypeMirror, List<AttachedElementInfo>> nestedMap = new HashMap<>();

        AttachedElementInfo streetField = createAttachedElement("street", TypeMapper.STRING, "getStreet");

        // City is also nested
        TypeMirror cityTypeMirror = mock(TypeMirror.class);
        when(cityTypeMirror.toString()).thenReturn("City");
        AttachedElementInfo cityField = createNestedAttachedElement("city", cityTypeMirror, "getCity");

        // Address has street (simple) and city (nested)
        nestedMap.put(addressTypeMirror, List.of(streetField, cityField));

        // Add elements for City (name and zipCode are simple fields)
        AttachedElementInfo cityNameField = createAttachedElement("name", TypeMapper.STRING, "getName");
        AttachedElementInfo zipCodeField = createAttachedElement("zipCode", TypeMapper.STRING, "getZipCode");
        nestedMap.put(cityTypeMirror, List.of(cityNameField, zipCodeField));

        personParam.setNestedElementInfo(nestedMap);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(personParam), false);

        String result = generator.generate();

        // Expected output structure:
        // person.getName() - simple field on person
        // person.getAddress().getStreet() - nested path to street
        // person.getAddress().getCity().getName() - deeply nested path to city name
        // person.getAddress().getCity().getZipCode() - deeply nested path to zip code
        System.out.println(result);
        assertTrue(result.contains("person.getName()"), "Should contain person.getName()");
        assertTrue(result.contains("person.getAddress().getStreet()"), "Should contain person.getAddress().getStreet()");
        assertTrue(result.contains("person.getAddress().getCity().getName()"), "Should contain deeply nested city name");
        assertTrue(result.contains("person.getAddress().getCity().getZipCode()"), "Should contain deeply nested zip code");
    }

    @Test
    void shouldHandleEmptyParameterList() {
        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(), false);

        String result = generator.generate();

        assertEquals("", result);
    }

    @Test
    void shouldHandleNullParameterList() {
        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(null, false);

        String result = generator.generate();

        assertEquals("", result);
    }

    @Test
    void shouldHandleAllPrimitiveTypes() {
        // Test all primitive types to ensure proper JDBC setter methods
        ElementInfo byteParam = createSimpleParameter("b", TypeMapper.BYTE);
        ElementInfo shortParam = createSimpleParameter("s", TypeMapper.SHORT);
        ElementInfo intParam = createSimpleParameter("i", TypeMapper.INTEGER);
        ElementInfo longParam = createSimpleParameter("l", TypeMapper.LONG);
        ElementInfo floatParam = createSimpleParameter("f", TypeMapper.FLOAT);
        ElementInfo doubleParam = createSimpleParameter("d", TypeMapper.DOUBLE);
        ElementInfo boolParam = createSimpleParameter("bool", TypeMapper.BOOLEAN);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(byteParam, shortParam, intParam, longParam, floatParam, doubleParam, boolParam), false);

        String result = generator.generate();

        assertTrue(result.contains("setByte"));
        assertTrue(result.contains("setShort"));
        assertTrue(result.contains("setInt"));
        assertTrue(result.contains("setLong"));
        assertTrue(result.contains("setFloat"));
        assertTrue(result.contains("setDouble"));
        assertTrue(result.contains("setBoolean"));
    }

    @Test
    void shouldHandleAllWrapperTypes() {
        // Test wrapper types
        ElementInfo byteParam = createSimpleParameter("b", TypeMapper.BYTE_WRAPPER);
        ElementInfo shortParam = createSimpleParameter("s", TypeMapper.SHORT_WRAPPER);
        ElementInfo intParam = createSimpleParameter("i", TypeMapper.INTEGER_WRAPPER);
        ElementInfo longParam = createSimpleParameter("l", TypeMapper.LONG_WRAPPER);
        ElementInfo floatParam = createSimpleParameter("f", TypeMapper.FLOAT_WRAPPER);
        ElementInfo doubleParam = createSimpleParameter("d", TypeMapper.DOUBLE_WRAPPER);
        ElementInfo boolParam = createSimpleParameter("bool", TypeMapper.BOOLEAN_WRAPPER);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(byteParam, shortParam, intParam, longParam, floatParam, doubleParam, boolParam), false);

        String result = generator.generate();

        assertTrue(result.contains("setByte"));
        assertTrue(result.contains("setShort"));
        assertTrue(result.contains("setInt"));
        assertTrue(result.contains("setLong"));
        assertTrue(result.contains("setFloat"));
        assertTrue(result.contains("setDouble"));
        assertTrue(result.contains("setBoolean"));
    }

    @Test
    void shouldHandleBigDecimalAndBigInteger() {
        ElementInfo bigDecParam = createSimpleParameter("decimal", TypeMapper.BIG_DECIMAL);
        ElementInfo bigIntParam = createSimpleParameter("bigInt", TypeMapper.BIG_INTEGER);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(bigDecParam, bigIntParam), false);

        String result = generator.generate();

        assertTrue(result.contains("setBigDecimal"));
        assertEquals(2, result.split("setBigDecimal").length - 1); // Both should use setBigDecimal
    }

    // EDGE CASE: Testing for null getter scenario
    // NOTE: According to PlsqlParamBinderGenerator:100-102, a null getter should throw IllegalStateException
    // This is a potential issue in the underlying code that should be documented
    @Test
    void shouldThrowExceptionWhenAttachedElementHasNullGetter() {
        ComposedElementInfo composedParam = createComposedParameter("person", "Person");

        // Create an attached element with null getter (edge case)
        AttachedElementInfo fieldWithoutGetter = new AttachedElementInfo();
        fieldWithoutGetter.setName("fieldWithoutGetter");

        TypeInfo typeInfo = mock(TypeInfo.class);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(TypeMapper.STRING);
        fieldWithoutGetter.setTypeInfo(typeInfo);
        fieldWithoutGetter.setGetter(null); // This will cause the issue

        composedParam.addElement(fieldWithoutGetter);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(List.of(composedParam), false);

        // BUG: This should throw IllegalStateException according to line 102
        // "This field does not have a getter " + attachedElementInfo.getName()
        IllegalStateException exception = assertThrows(IllegalStateException.class, generator::generate);

        assertTrue(exception.getMessage().contains("This field does not have a getter"));
        assertTrue(exception.getMessage().contains("fieldWithoutGetter"));
    }

    @Test
    void shouldHandleMixedSimpleAndComposedParameters() {
        // Mix of simple and composed parameters
        ElementInfo simpleParam1 = createSimpleParameter("id", TypeMapper.LONG);

        ComposedElementInfo composedParam = createComposedParameter("address", "Address");
        AttachedElementInfo streetField = createAttachedElement("street", TypeMapper.STRING, "getStreet");
        composedParam.addElement(streetField);

        ElementInfo simpleParam2 = createSimpleParameter("active", TypeMapper.BOOLEAN);

        PlsqlParamBinderGenerator generator = new PlsqlParamBinderGenerator(
                List.of(simpleParam1, composedParam, simpleParam2), false);

        String result = generator.generate();

        String expected = "stmt.setLong(pos++,id);\n" +
                "stmt.setString(pos++,address.getStreet());\n" +
                "stmt.setBoolean(pos++,active);";
        assertEquals(expected, result);
    }

    // Helper methods to create test objects
    private ElementInfo createSimpleParameter(String name, TypeMapper typeMapper) {
        ElementInfo param = new ElementInfo();
        param.setName(name);

        TypeInfo typeInfo = mock(TypeInfo.class);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(typeMapper);

        param.setTypeInfo(typeInfo);
        return param;
    }

    private ComposedElementInfo createComposedParameter(String name, String typeName) {
        ComposedElementInfo param = new ComposedElementInfo();
        param.setName(name);

        TypeMirror typeMirror = mock(TypeMirror.class);
        when(typeMirror.toString()).thenReturn(typeName);

        TypeInfo typeInfo = mock(TypeInfo.class);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.getMirror()).thenReturn(typeMirror);

        param.setTypeInfo(typeInfo);
        return param;
    }

    private AttachedElementInfo createAttachedElement(String name, TypeMapper typeMapper, String getterName) {
        AttachedElementInfo element = new AttachedElementInfo();
        element.setName(name);

        TypeInfo typeInfo = mock(TypeInfo.class);
        when(typeInfo.isSimple()).thenReturn(true);
        when(typeInfo.asTypeMapper()).thenReturn(typeMapper);

        element.setTypeInfo(typeInfo);

        // Mock getter
        ExecutableElement getter = mock(ExecutableElement.class);
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn(getterName);
        when(getter.getSimpleName()).thenReturn(methodName);
        when(getter.toString()).thenReturn(getterName + "()");
        element.setGetter(getter);

        return element;
    }

    private AttachedElementInfo createNestedAttachedElement(String name, TypeMirror typeMirror, String getterName) {
        AttachedElementInfo element = new AttachedElementInfo();
        element.setName(name);

        TypeInfo typeInfo = mock(TypeInfo.class);
        when(typeInfo.isSimple()).thenReturn(false);
        when(typeInfo.getMirror()).thenReturn(typeMirror);

        element.setTypeInfo(typeInfo);

        // Mock getter
        ExecutableElement getter = mock(ExecutableElement.class);
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn(getterName);
        when(getter.getSimpleName()).thenReturn(methodName);
        when(getter.toString()).thenReturn(getterName + "()");
        element.setGetter(getter);

        return element;
    }
}