package com.plsql.tools.tools.fields.info;

import javax.lang.model.element.Element;

public interface Info {
    boolean isSimple();
    Element getField();

    boolean isWrapped();
}
