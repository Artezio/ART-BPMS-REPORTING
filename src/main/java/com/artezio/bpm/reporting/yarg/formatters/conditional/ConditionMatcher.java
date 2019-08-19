package com.artezio.bpm.reporting.yarg.formatters.conditional;

import com.artezio.bpm.reporting.yarg.formatters.el.ELEvaluator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionMatcher {

    private static final String IF_GROUP = "\\$\\{IF\\s?(.*?)}";
    private static final String ELSE_MARKER = "\\$\\{ELSE}";
    private static final String ENDIF_MARKER = "\\$\\{ENDIF}";
    private static final String TEXT_GROUP = "(.*?)";

    private static final Pattern IF_ELSE_ENDIF_PATTERN = Pattern.compile(IF_GROUP + TEXT_GROUP + ELSE_MARKER + TEXT_GROUP + ENDIF_MARKER);
    private static final Pattern IF_ENDIF_PATTERN = Pattern.compile(IF_GROUP + TEXT_GROUP + ENDIF_MARKER);

    private Matcher ifElseEndifMatcher;
    private Matcher ifEndifMatcher;
    private boolean hasElse;

    public static boolean isConditionalMarker(String string) {
        return "IF".equals(string) || "ELSE".equals(string) || "ENDIF".equals(string);
    }

    public static String replaceConditionsInText(String text) {
        ConditionMatcher matcher = new ConditionMatcher();
        while (matcher.match(text)) {
            String condition = matcher.getCondition();
            // As per com.haulmont.yarg.formatters.impl.docx.TextWrapper#fillTextWithBandData
            // Alias rendering would be skipped inside tables, by checking the alias regex pattern
            // This would leave ${...} aliases inside EL expression, so presence of ${} is the only way to check if we
            // are inside a table and skip calculation of EL value, because bands are not yet calculated by Yarg
            if (condition.matches(".*?\\$\\{.*?")) {
                break;
            }
            Boolean conditionEvaluationResult = ELEvaluator.evaluateElExpression("${" + condition + "}", Boolean.class);
            text = matcher.getResultText(conditionEvaluationResult);
        }
        return text;
    }

    String getResultText(Boolean conditionEvaluationResult) {
        if (conditionEvaluationResult) {
            if (hasElse) {
                return ifElseEndifMatcher.replaceFirst("$2");
            } else {
                return ifEndifMatcher.replaceFirst("$2");
            }
        } else {
            if (hasElse) {
                return ifElseEndifMatcher.replaceFirst("$3");
            } else {
                return "";
            }
        }
    }

    public boolean match(String text) {
        ifElseEndifMatcher = IF_ELSE_ENDIF_PATTERN.matcher(text);
        ifEndifMatcher = IF_ENDIF_PATTERN.matcher(text);
        if (ifElseEndifMatcher.find()) {
            hasElse = true;
            return true;
        } else {
            if (ifEndifMatcher.find()) {
                hasElse = false;
                return true;
            } else {
                return false;
            }
        }
    }

    private String getCondition() {
        if (hasElse) {
            return ifElseEndifMatcher.group(1);
        } else {
            return ifEndifMatcher.group(1);
        }
    }
}