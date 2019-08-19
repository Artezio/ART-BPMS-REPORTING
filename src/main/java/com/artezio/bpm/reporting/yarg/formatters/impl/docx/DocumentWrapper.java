package com.artezio.bpm.reporting.yarg.formatters.impl.docx;

import com.haulmont.yarg.formatters.impl.DocxFormatterDelegate;
import org.docx4j.TraversalUtil;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

public class DocumentWrapper extends com.haulmont.yarg.formatters.impl.docx.DocumentWrapper {

    public DocumentWrapper(DocxFormatterDelegate docxFormatter, WordprocessingMLPackage wordprocessingMLPackage) {
        super(docxFormatter, wordprocessingMLPackage);
    }

    @Override
    protected void collectTexts() {
        TextVisitor textVisitor = new TextVisitor(docxFormatter);
        new TraversalUtil(mainDocumentPart, textVisitor);
        texts = textVisitor.getTextWrappers();
    }
}

