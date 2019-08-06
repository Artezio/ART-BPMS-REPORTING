package com.artezio.reporting.yarg.formatters.impl;

import com.artezio.reporting.yarg.formatters.conditional.ConditionMatcher;
import com.artezio.reporting.yarg.formatters.el.ELEvaluator;
import com.haulmont.yarg.exception.OpenOfficeException;
import com.haulmont.yarg.exception.ReportingException;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.doc.connector.OfficeIntegrationAPI;
import com.haulmont.yarg.structure.BandData;
import com.sun.star.container.XIndexAccess;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.util.XReplaceable;
import com.sun.star.util.XSearchDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.haulmont.yarg.formatters.impl.doc.UnoConverter.as;

/**
 * Orignal class had no other way of delegating calls other that reimplementing its methods
 * This class has only minor changes marked accodrindly in comments to support Conditional and EL expressions
 */
public class DocFormatter extends com.haulmont.yarg.formatters.impl.DocFormatter {
    public DocFormatter(FormatterFactoryInput formatterFactoryInput, OfficeIntegrationAPI officeIntegration) {
        super(formatterFactoryInput, officeIntegration);
    }

    @Override
    protected void replaceAllAliasesInDocument() {
        replaceAllAliasesExceptConditionsInDocument();
        XIndexAccess documentParts = getDocumentParts();
        for (int i = 0; i < documentParts.getCount(); i++) {
            try {
                XTextRange textRange = as(XTextRange.class, documentParts.getByIndex(i));
                String text = textRange.getString();
// -- Evaluate EL expressions and conditions
                text = ELEvaluator.evaluateExpressionsInText(text);
                textRange.setString(ConditionMatcher.replaceConditionsInText(text));
            } catch (IndexOutOfBoundsException | WrappedTargetException e) {
                throw new RuntimeException("Could not generate Doc report", e);
            }
        }
    }

    protected void replaceAllAliasesExceptConditionsInDocument() {
        XTextDocument xTextDocument = as(XTextDocument.class, xComponent);
        XReplaceable xReplaceable = as(XReplaceable.class, xTextDocument);
        XSearchDescriptor searchDescriptor = xReplaceable.createSearchDescriptor();
        searchDescriptor.setSearchString(ALIAS_WITH_BAND_NAME_REGEXP);
        try {
            searchDescriptor.setPropertyValue(SEARCH_REGULAR_EXPRESSION, true);
        } catch (Exception e) {
            throw new OpenOfficeException("An error occurred while setting search properties in Open office", e);
        }

        XIndexAccess indexAccess = xReplaceable.findAll(searchDescriptor);
        for (int i = 0; i < indexAccess.getCount(); i++) {
            try {
                XTextRange textRange = as(XTextRange.class, indexAccess.getByIndex(i));
                String alias = unwrapParameterName(textRange.getString());

                BandPathAndParameterName bandAndParameter = separateBandNameAndParameterName(alias);
// --- Skip conditional markers
                if (ConditionMatcher.isConditionalMarker(bandAndParameter.getParameterName())) {
                    continue;
                }
                BandData band = findBandByPath(bandAndParameter.getBandPath());
                if (band != null) {
                    insertValue(textRange.getText(), textRange, band, bandAndParameter.getParameterName());
                } else {
                    throw wrapWithReportingException(String.format("No band for alias [%s] found", alias));
                }
            } catch (ReportingException e) {
                throw e;
            } catch (Exception e) {
                throw wrapWithReportingException(String.format("An error occurred while replacing aliases in document. Regexp [%s]. Replacement number [%d]", ALIAS_WITH_BAND_NAME_REGEXP, i), e);
            }
        }
    }

    @Override
    protected void fillCell(BandData band, XCell xCell) {
        checkThreadInterrupted();
        XText xText = as(XText.class, xCell);
        String cellText = xText.getString();
        cellText = cellText.replace("\r\n", "\n");//just a workaround for Windows \r\n break symbol
        List<String> parametersToInsert = new ArrayList<String>();
        Matcher matcher = UNIVERSAL_ALIAS_PATTERN.matcher(cellText);
        while (matcher.find()) {
            String parameterName = unwrapParameterName(matcher.group());
// -- Skip condition markers
            if (!ConditionMatcher.isConditionalMarker(parameterName)) {
                parametersToInsert.add(parameterName);
            }
        }
        for (String parameterName : parametersToInsert) {
            XTextCursor xTextCursor = xText.createTextCursor();

            String paramStr = "${" + parameterName + "}";
            int index = cellText.indexOf(paramStr);
            while (index >= 0) {
                xTextCursor.gotoStart(false);
                xTextCursor.goRight((short) (index + paramStr.length()), false);
                xTextCursor.goLeft((short) paramStr.length(), true);

                insertValue(xText, xTextCursor, band, parameterName);
                cellText = formatCellText(xText.getString());

                index = cellText.indexOf(paramStr);
            }
        }
    }

// -- Compose entire document text to replace all expressions
    private XIndexAccess getDocumentParts() {
        XTextDocument xTextDocument = as(XTextDocument.class, xComponent);
        XReplaceable xReplaceable = as(XReplaceable.class, xTextDocument);
        XSearchDescriptor searchDescriptor = xReplaceable.createSearchDescriptor();
        searchDescriptor.setSearchString(".*");
        try {
            searchDescriptor.setPropertyValue(SEARCH_REGULAR_EXPRESSION, true);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate Doc report", e);
        }
        return xReplaceable.findAll(searchDescriptor);
    }

}
