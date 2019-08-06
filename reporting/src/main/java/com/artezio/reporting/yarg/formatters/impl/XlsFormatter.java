package com.artezio.reporting.yarg.formatters.impl;

import com.artezio.reporting.yarg.formatters.conditional.ConditionAwareBandDataInserter;
import com.artezio.reporting.yarg.formatters.conditional.ConditionMatcher;
import com.artezio.reporting.yarg.formatters.el.ELEvaluator;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.XLSFormatter;
import com.haulmont.yarg.structure.BandData;
import org.apache.poi.hssf.usermodel.HSSFCell;

public class XlsFormatter extends XLSFormatter {
    public XlsFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
    }

    @Override
    protected String inlineBandDataToCellString(HSSFCell cell, String templateCellValue, BandData band) {
        String textWithInlinedBands = super.inlineBandDataToCellString(cell, templateCellValue, band);
        String textWithEvaluatedExpressions = ELEvaluator.evaluateExpressionsInText(textWithInlinedBands);
        return ConditionMatcher.replaceConditionsInText(textWithEvaluatedExpressions);
    }

    @Override
    protected String insertBandDataToString(BandData bandData, String resultStr) {
        String stringWithReplacedBands = new ConditionAwareBandDataInserter().insertBandDataToString(
                bandData,
                resultStr,
                this::unwrapParameterName,
                this::formatValue,
                this::inlineParameterValue);
        return ELEvaluator.evaluateExpressionsInText(stringWithReplacedBands);
    }
}
