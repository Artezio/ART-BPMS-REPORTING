package com.artezio.bpm.reporting.yarg.formatters.impl;

import com.artezio.bpm.reporting.yarg.formatters.conditional.ConditionAwareBandDataInserter;
import com.artezio.bpm.reporting.yarg.formatters.conditional.ConditionMatcher;
import com.artezio.bpm.reporting.yarg.formatters.el.ELEvaluator;
import com.artezio.bpm.reporting.yarg.formatters.impl.docx.DocumentWrapper;
import com.artezio.bpm.reporting.yarg.formatters.impl.docx.TextWrapper;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.DocxFormatterDelegate;
import com.haulmont.yarg.structure.BandData;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

public class DocxFormatter extends com.haulmont.yarg.formatters.impl.DocxFormatter {

    public DocxFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
    }

    protected void loadDocument() {
        if (reportTemplate == null)
            throw new NullPointerException("Template file can't be null.");
        try {
            wordprocessingMLPackage = WordprocessingMLPackage.load(reportTemplate.getDocumentContent());
            documentWrapper = new DocumentWrapper(new DocxFormatterDelegate(this), wordprocessingMLPackage);
        } catch (Docx4JException e) {
            throw wrapWithReportingException(String.format("An error occurred while reading docx template. File name [%s]", reportTemplate.getDocumentName()), e);
        }
    }

    @Override
    protected void replaceAllAliasesInDocument() {
        for (com.haulmont.yarg.formatters.impl.docx.TextWrapper text : documentWrapper.getTexts()) {
            if (text instanceof TextWrapper) {
                ((TextWrapper) text).replaceExpressionsInText();
            } else {
                text.fillTextWithBandData();
            }
        }
    }

    @Override
    protected String insertBandDataToString(BandData bandData, String resultStr) {
        String stringWithInsertedBands = insertBandData(bandData, resultStr);
        String stringWithEvaluatedExpressions = ELEvaluator.evaluateExpressionsInText(stringWithInsertedBands);
        return ConditionMatcher.replaceConditionsInText(stringWithEvaluatedExpressions);
    }

    private String insertBandData(BandData bandData, String resultStr) {
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
