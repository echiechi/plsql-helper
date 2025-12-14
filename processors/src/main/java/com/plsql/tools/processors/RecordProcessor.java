package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.Extractor;
import com.plsql.tools.tools.fields.FieldMethodExtractor;
import com.plsql.tools.tools.fields.info.FieldInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordProcessor {
    private final ProcessingContext context;
    public RecordProcessor(ProcessingContext context) {
        this.context = context;
    }

    public void process(Element record) {
        context.logInfo("Start Processing and cashing of", record.getSimpleName());
        Extractor.getInstance().extractClassInfo(record); // extract useful information to reuse later
    }
}
