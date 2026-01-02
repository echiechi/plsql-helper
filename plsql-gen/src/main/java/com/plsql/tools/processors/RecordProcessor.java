package com.plsql.tools.processors;

import com.plsql.tools.ProcessingContext;
import com.plsql.tools.tools.extraction.Extractor;

import javax.lang.model.element.Element;

public class RecordProcessor {
    private final ProcessingContext context;

    private final Extractor extractor;

    public RecordProcessor(ProcessingContext context) {
        this.context = context;
        this.extractor = new Extractor(context);
    }

    public void  process(Element record) {
        context.logInfo("Start Processing and cashing of", record.getSimpleName());
        this.extractor.extractClassInfoAndAlimCache(record); // extract useful information to reuse later
        context.logInfo("loaaging info");
    }
}
