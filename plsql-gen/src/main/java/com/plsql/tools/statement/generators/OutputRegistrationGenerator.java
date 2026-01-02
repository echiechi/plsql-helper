package com.plsql.tools.statement.generators;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.statement.Generator;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.sql.JDBCType;
import java.util.List;

public class OutputRegistrationGenerator implements Generator {

    private final List<ReturnElementInfo> outputList;

    public OutputRegistrationGenerator(List<ReturnElementInfo> outputList) {
        this.outputList = outputList;
    }

    @Override
    public String generate() {
        if (outputList == null) {
            return "";
        }
        StringBuilder statements = new StringBuilder();
        String pos = "%s++".formatted(CodeGenConstants.POSITION_VAR);
        for (int i = 0; i < outputList.size(); i++) {
            var output = outputList.get(i);
            if (i == outputList.size() - 1) {
                pos = CodeGenConstants.POSITION_VAR;
            }
            TypeMapper type = null;
            if (output.getTypeInfo().isWrapped()) {
                type = TypeMapper.fromSimpleName(output.getTypeInfo().getWrappedType().toString());
            }
            if (type == null) {
                type = output.getTypeInfo().asTypeMapper();
            }
            statements.append(String.format("stmt.registerOutParameter(%s, JDBCType.%s);", pos, (type == null) ?
                            JDBCType.REF_CURSOR.name() : type.getJdbcType().name()))
                    .append("\n");
        }
        return statements.toString();
    }
}
