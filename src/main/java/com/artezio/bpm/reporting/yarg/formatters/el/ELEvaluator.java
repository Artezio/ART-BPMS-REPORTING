package com.artezio.bpm.reporting.yarg.formatters.el;

import com.artezio.bpm.reporting.yarg.formatters.conditional.ConditionMatcher;

import javax.el.ExpressionFactory;
import javax.el.StandardELContext;
import javax.el.ValueExpression;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ELEvaluator {

    private static final String TEXT_GROUP = "(.*?)";
    private static final String NOT_CONDITIONAL_MARKER_GROUP = "(?!(IF|ELSE|ENDIF))";
    private static final String EL_EXPRESSION_GROUP = "#\\{" + NOT_CONDITIONAL_MARKER_GROUP + TEXT_GROUP + "}";

    private static final Pattern EL_EXPRESSION_PATTERN = Pattern.compile(EL_EXPRESSION_GROUP);

    public static boolean hasElExpression(String string) {
        return EL_EXPRESSION_PATTERN.matcher(string).find();
    }

    public static boolean hasCompleteExpression(String string) {
        if (!string.contains("#{")) return false;
        int expressionStartPosition = string.indexOf("#{");
        long leftBracketsCount = string.chars()
                .skip(expressionStartPosition)
                .filter(ch -> ch == '{')
                .count();
        long rightBracketsCount = string.chars()
                .skip(expressionStartPosition)
                .filter(ch -> ch == '}')
                .count();
        return (leftBracketsCount <= rightBracketsCount) && hasElExpression(string);
    }

    public static String evaluateExpressionsInText(String text) {
        Matcher expressionMatcher = EL_EXPRESSION_PATTERN.matcher(text);
        while (expressionMatcher.find()) {
            String expression = expressionMatcher.group(2);
            // As per com.haulmont.yarg.formatters.impl.docx.TextWrapper#fillTextWithBandData
            // Alias rendering would be skipped inside tables, by checking the alias regex pattern
            // This would leave ${...} aliases inside EL expression, so presence of ${} is the only way to check if we
            // are inside a table and skip calculation of EL value, because bands are not yet calculated by Yarg
            if (expression.matches(".*?\\$\\{.*?")) {
                break;
            }
            Object conditionEvaluationResult = ELEvaluator.evaluateElExpression("${" + expression + "}", Object.class);
            text = expressionMatcher.replaceFirst(conditionEvaluationResult.toString());
            expressionMatcher.reset(text);
        }
        return text;
    }

    public static <T> T evaluateElExpression(String elExpression, Class<T> resultClass) {
        BeanManager beanManager = CDI.current().getBeanManager();
        ExpressionFactory expressionFactory = beanManager.wrapExpressionFactory(ExpressionFactory.newInstance());
        StandardELContext elContext = new StandardELContext(expressionFactory);
        elContext.addELResolver(beanManager.getELResolver());
        ValueExpression expression = expressionFactory.createValueExpression(elContext, elExpression, resultClass);
        return (T) expression.getValue(elContext);
    }
}
