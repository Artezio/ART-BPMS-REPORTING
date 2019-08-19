package com.artezio.bpm.reporting.yarg.formatters.impl.docx;

import com.artezio.bpm.reporting.yarg.formatters.conditional.ConditionMatcher;
import com.artezio.bpm.reporting.yarg.formatters.el.ELEvaluator;
import com.haulmont.yarg.formatters.impl.DocxFormatterDelegate;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextVisitor extends com.haulmont.yarg.formatters.impl.docx.TextVisitor {

    public TextVisitor(DocxFormatterDelegate docxFormatter) {
        super(docxFormatter);
    }

    public Set<com.haulmont.yarg.formatters.impl.docx.TextWrapper> getTextWrappers() {
        return textWrappers;
    }

    private class TextMerger extends com.haulmont.yarg.formatters.impl.docx.TextMerger {

        private final boolean hasElExpressions;
        private final boolean hasConditions;

        public TextMerger(ContentAccessor paragraph, String regexp, boolean hasElExpressions, boolean hasConditions) {
            super(paragraph, regexp);
            this.hasElExpressions = hasElExpressions;
            this.hasConditions = hasConditions;
        }

        @Override
        protected void handleText(Text currentText) {
            if (startText == null) {
                startText = currentText;
                mergedTexts = new HashSet<>();
                mergedTextsString = new StringBuilder();
            }
            if (startText != null) {
                addToMergeQueue(currentText);
                if (mergeQueueMatchesRegexp()) {
                    handleMatchedText();
                }
            }
        }

        @Override
        protected boolean mergeQueueMatchesRegexp() {
            boolean matches = true;
            String text = mergedTextsString.toString();
            if (hasConditions) matches = new ConditionMatcher().match(text);
            if (hasElExpressions) matches = matches && ELEvaluator.hasCompleteExpression(text);
            return matches;
        }

    }

    @Override
    public List<Object> apply(Object o) {
        if (o instanceof P || o instanceof P.Hyperlink) {
            String paragraphText = docxFormatter.getElementText(o);
            boolean hasConditions = new ConditionMatcher().match(paragraphText);
            boolean hasElExpressions = ELEvaluator.hasElExpression(paragraphText);
            if (hasConditions || hasElExpressions) {
                // Merge text elements this paragraph to get condition or EL expression block as single piece of text
                Set<Text> mergedTexts = new TextMerger((ContentAccessor) o, ".*", hasElExpressions, hasConditions).mergeMatchedTexts();
                for (Text text : mergedTexts) {
                    handle(text);
                }
            } else {
                return super.apply(o);
            }
        }
        return null;
    }

    @Override
    protected void handle(Text text) {
        ConditionMatcher conditionMatcher = new ConditionMatcher();
        if (conditionMatcher.match(text.getValue()) || ELEvaluator.hasElExpression(text.getValue())) {
            TextWrapper textWrapper = new TextWrapper(docxFormatter, text);
            textWrappers.add(textWrapper);
        } else {
            super.handle(text);
        }
    }
}
