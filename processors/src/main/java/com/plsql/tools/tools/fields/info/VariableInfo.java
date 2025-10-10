package com.plsql.tools.tools.fields.info;

import com.plsql.tools.annotations.Input;
import com.plsql.tools.annotations.Output;
import com.plsql.tools.enums.JdbcHelper;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import java.util.List;

public class VariableInfo implements Info {
    protected Element field;

    private String customName;

    private List<Output> outputs;

    public VariableInfo(Element field) {
        this.field = field;
    }

    @Override
    public boolean isSimple() {
        return field.asType().getKind().isPrimitive() ||
                getTypeName().startsWith("java.lang.")
                || getTypeName().startsWith("java.time.")
                || getTypeName().equals("java.util.Date")
                || getTypeName().equals("java.math.BigDecimal")
                || getTypeName().equals("java.math.BigInteger");
    }

    @Override
    public boolean isWrapped() {
        return false;
    }

    @Override
    public Element getField() {
        return field;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getName() {
        return (customName == null) ? field.getSimpleName().toString() : customName;
    }

    public String getTypeName() {
        return field.asType().toString();
    }

    public JdbcHelper getJdbcMappedType() {
        return JdbcHelper.fromSimpleName(getTypeName());
    }

    public String inputName() {
        Input input = field.getAnnotation(Input.class);
        return input != null && StringUtils.isNotBlank(input.value()) ? input.value() : getName();
    }
    public List<Output> getOutputs() {
        return outputs;
    }
    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return "VariableInfo{" +
                "field=" + field +
                ", customName='" + customName + '\'' +
                ", output=" + outputs +
                '}';
    }
}
