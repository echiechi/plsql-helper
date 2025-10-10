package com.plsql.tools.statement.generators;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.enums.JdbcHelper;
import com.plsql.tools.tools.Tools;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;

import static com.plsql.tools.tools.ElementTools.OutputElement;

public class OutRegistrationGenerator {
    private final ProcessingContext context;

    public OutRegistrationGenerator(ProcessingContext context) {
        this.context = context;
    }


    // TODO : get from return class annotation instead
    public List<String> generateOutStatements(List<OutputElement> outputs) {
        List<String> statements = new ArrayList<>();
        String pos = "pos++";
        for (int i = 0; i < outputs.size(); i++) {
            OutputElement output = outputs.get(i);
            if (i == outputs.size() - 1) {
                pos = "pos";
            }
            var type = JdbcHelper.fromSimpleName(output.element.asType().toString());
            if (type == null) {
                type = JdbcHelper.fromSimpleName(Tools.getTypeElement(context, output.element).toString());
            }
            if (output.isWrapped()) {
                type = JdbcHelper.fromSimpleName(output.wrappedType.toString());
            }
            statements.add(String.format("stmt.registerOutParameter(%s, JDBCType.%s);", pos, (type == null) ?
                    JDBCType.REF_CURSOR.name() : type.getJdbcType().name()));
        }
        return statements;
    }
}
