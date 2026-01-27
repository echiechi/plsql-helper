package com.plsql.tools.statements.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ParameterTypeTest {

    @Test
    public void testGetSqlType() {
        assertEquals("IN", ParameterType.IN.getSqlType());
        assertEquals("OUT", ParameterType.OUT.getSqlType());
        assertEquals("IN OUT", ParameterType.IN_OUT.getSqlType());
    }

    @Test
    public void testEnumValues() {
        ParameterType[] values = ParameterType.values();
        assertEquals(3, values.length);
        
        assertNotNull(ParameterType.valueOf("IN"));
        assertNotNull(ParameterType.valueOf("OUT"));
        assertNotNull(ParameterType.valueOf("IN_OUT"));
    }

    @Test
    public void testToString() {
        assertEquals("IN", ParameterType.IN.toString());
        assertEquals("OUT", ParameterType.OUT.toString());
        assertEquals("IN_OUT", ParameterType.IN_OUT.toString());
    }
}