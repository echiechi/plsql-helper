package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.fields.FieldMethodExtractor;
import com.plsql.tools.tools.fields.info.FieldInfo;
import com.plsql.tools.tools.fields.info.VariableInfo;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordProcessor {

    private final ProcessingContext context;
    private final FieldMethodExtractor extractor;

    public RecordProcessor(ProcessingContext context) {
        this.context = context;
        this.extractor = FieldMethodExtractor.getInstance(context);
    }

    public void process(Element record) {
        context.logInfo("Start Processing and cashing of", record.getSimpleName());
        Set<FieldInfo> extractedInformation = extractor.extractClassInfo((TypeElement) record);
        if(extractedInformation.isEmpty()){
            context.logWarning("No information extracted from this record");
        }
        context.logInfo("Extracted fields:", extractedInformation.stream()
                .map(VariableInfo::getName).collect(Collectors.joining(" | ")));
    }
}
