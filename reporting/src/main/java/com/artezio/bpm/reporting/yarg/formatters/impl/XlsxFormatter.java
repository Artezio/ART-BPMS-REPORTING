package com.artezio.bpm.reporting.yarg.formatters.impl;

import com.artezio.bpm.reporting.yarg.formatters.conditional.ConditionAwareBandDataInserter;
import com.artezio.bpm.reporting.yarg.formatters.conditional.ConditionMatcher;
import com.artezio.bpm.reporting.yarg.formatters.el.ELEvaluator;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.structure.BandData;

public class XlsxFormatter extends com.haulmont.yarg.formatters.impl.XlsxFormatter {
    public XlsxFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
    }

    @Override
    protected String insertBandDataToString(BandData bandData, String resultStr) {
        String stringWithInsertedBandData = new ConditionAwareBandDataInserter().insertBandDataToString(
                bandData,
                resultStr,
                this::unwrapParameterName,
                this::formatValue,
                this::inlineParameterValue);
        String stringWithEvaluatedExpressions = ELEvaluator.evaluateExpressionsInText(stringWithInsertedBandData);
        return ConditionMatcher.replaceConditionsInText(stringWithEvaluatedExpressions);
    }
}
