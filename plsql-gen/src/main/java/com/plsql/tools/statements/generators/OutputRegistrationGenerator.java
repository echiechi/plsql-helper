package com.plsql.tools.statements.generators;

import com.plsql.tools.enums.TypeMapper;
import com.plsql.tools.statements.Generator;
import com.plsql.tools.tools.CodeGenConstants;
import com.plsql.tools.tools.extraction.info.ReturnElementInfo;

import java.sql.JDBCType;
import java.util.List;

import static com.plsql.tools.tools.GenTools.incrementVar;
import static com.plsql.tools.tools.GenTools.registerOutParameter;
import static com.plsql.tools.tools.CodeGenConstants.STATEMENT_VAR;

public class OutputRegistrationGenerator implements Generator {

    private final List<ReturnElementInfo> outputList;

    public OutputRegistrationGenerator(List<ReturnElementInfo> outputList) {
        this.outputList = outputList;
    }

    @Override
    public String generate() {
        if (outputList == null || outputList.isEmpty()) {
            return "";
        }
        StringBuilder statements = new StringBuilder();
        String pos = incrementVar(CodeGenConstants.POSITION_VAR);
        for (int i = 0; i < outputList.size(); i++) {
            var output = outputList.get(i);
            if (i == outputList.size() - 1) {
                pos = CodeGenConstants.POSITION_VAR;
            }
            TypeMapper type;
            if (output.getTypeInfo().isWrapped()) {
                type = output.getTypeInfo().wrappedTypeAsTypeMapper();
            } else {
                type = output.getTypeInfo().asTypeMapper();
            }
            var jdbcType = (type == null) ?
                    JDBCType.REF_CURSOR.name() : type.getJdbcType().name();
            statements.append(registerOutParameter(STATEMENT_VAR, pos, jdbcType))
                    .append("\n");
        }
        return statements.toString();
    }
}
