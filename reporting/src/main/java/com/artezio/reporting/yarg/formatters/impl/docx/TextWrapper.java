package com.artezio.reporting.yarg.formatters.impl.docx;

import com.artezio.reporting.yarg.formatters.conditional.ConditionMatcher;
import com.artezio.reporting.yarg.formatters.el.ELEvaluator;
import com.haulmont.yarg.formatters.impl.DocxFormatterDelegate;
import org.docx4j.wml.Text;

public class TextWrapper extends com.haulmont.yarg.formatters.impl.docx.TextWrapper {

    public TextWrapper(DocxFormatterDelegate docxFormatterDelegate, Text text) {
        super(docxFormatterDelegate, text);
    }

    public void replaceExpressionsInText() {
        fillTextWithBandData();
        String textWithEvaluatedExpressions = ELEvaluator.evaluateExpressionsInText(text.getValue());
        String textWithReplacedConditions = ConditionMatcher.replaceConditionsInText(textWithEvaluatedExpressions);
        text.setValue(textWithReplacedConditions);
    }

}
