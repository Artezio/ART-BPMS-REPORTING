package com.artezio.bpm.reporting.yarg.formatters;

import com.artezio.bpm.reporting.yarg.formatters.impl.DocFormatter;
import com.artezio.bpm.reporting.yarg.formatters.impl.DocxFormatter;
import com.artezio.bpm.reporting.yarg.formatters.impl.XlsFormatter;
import com.artezio.bpm.reporting.yarg.formatters.impl.XlsxFormatter;
import com.haulmont.yarg.exception.UnsupportedFormatException;
import com.haulmont.yarg.formatters.factory.DefaultFormatterFactory;
import com.haulmont.yarg.formatters.impl.doc.connector.OfficeIntegration;

public class FormatterFactory extends DefaultFormatterFactory {
    private final static String OPENOFFICE_INSTALLATION_DIRECTORY = System.getenv("OPENOFFICE_INSTALLATION_DIRECTORY");

    public FormatterFactory() {
        if (OPENOFFICE_INSTALLATION_DIRECTORY != null) {
            officeIntegration = new OfficeIntegration(OPENOFFICE_INSTALLATION_DIRECTORY, 8100, 8101);
            ((OfficeIntegration) officeIntegration).setTimeoutInSeconds(7000);
        }
        FormatterCreator docCreator = factoryInput -> {
            if (officeIntegration == null) {
                throw new UnsupportedFormatException("Could not use doc templates because Open Office connection params not set. Please check, that \"OPENOFFICE_INSTALLATION_DIRECTORY\" environment variable is set.");
            }
            DocFormatter docFormatter = new DocFormatter(factoryInput, officeIntegration);
            docFormatter.setDefaultFormatProvider(defaultFormatProvider);
            return docFormatter;
        };
        formattersMap.put("odt", docCreator);
        formattersMap.put("doc", docCreator);
        formattersMap.put("docx", factoryInput -> {
            DocxFormatter docxFormatter = new DocxFormatter(factoryInput);
            docxFormatter.setDefaultFormatProvider(defaultFormatProvider);
            docxFormatter.setDocumentConverter(documentConverter);
            docxFormatter.setHtmlImportProcessor(htmlImportProcessor);
            docxFormatter.setScripting(scripting);
            return docxFormatter;
        });
        formattersMap.put("xls", factoryInput -> {
            XlsFormatter xlsFormatter = new XlsFormatter(factoryInput);
            xlsFormatter.setDocumentConverter(documentConverter);
            xlsFormatter.setDefaultFormatProvider(defaultFormatProvider);
            xlsFormatter.setScripting(scripting);
            return xlsFormatter;
        });
        FormatterCreator xlsxCreator = factoryInput -> {
            com.haulmont.yarg.formatters.impl.XlsxFormatter xlsxFormatter = new XlsxFormatter(factoryInput);
            xlsxFormatter.setDefaultFormatProvider(defaultFormatProvider);
            xlsxFormatter.setDocumentConverter(documentConverter);
            xlsxFormatter.setScripting(scripting);
            return xlsxFormatter;
        };
        formattersMap.put("xlsx", xlsxCreator);
        formattersMap.put("xlsm", xlsxCreator);
    }
}
