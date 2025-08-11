package com.plsql.tools.statement;

import java.util.Objects;

public class Parameter {
    private final String name;
    private final ParameterType type;

    public Parameter(String name, ParameterType type) {
        this.name = name;
        this.type = type != null ? type : ParameterType.IN;
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public String formatForCall() {
        return String.format("%s => ?", name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return Objects.equals(name, parameter.name) && type == parameter.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return String.format("Parameter{name='%s', type=%s}", name, type);
    }
}
