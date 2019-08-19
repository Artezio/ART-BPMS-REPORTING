package com.artezio.bpm.reporting.yarg.formatters.conditional;

import com.haulmont.yarg.structure.BandData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;

import static com.haulmont.yarg.formatters.impl.AbstractFormatter.UNIVERSAL_ALIAS_PATTERN;

/**
 * Inserts band data, in strings such as ${band.item.value}, but with regard to conditional markers
 */
public class ConditionAwareBandDataInserter {

    public interface ParameterValueFormatter {
        String format(Object value, String parameterName, String fullParameterName);
    }

    public interface ParameterInliner {
        String inline(String template, String parameterName, String value);
    }

    public String insertBandDataToString(
            BandData bandData,
            String string,
            Function<String, String> parameterNameUnwrapper,
            ParameterValueFormatter parameterValueFormatter,
            ParameterInliner parameterInliner) {
        List<String> parametersToInsert = getParametersToInsert(string, parameterNameUnwrapper);
        for (String parameterName : parametersToInsert) {
            Object value = bandData.getData().get(parameterName);
            String fullParameterName = bandData.getName() + "." + parameterName;
            String valueStr = parameterValueFormatter.format(value, parameterName, fullParameterName);
            string = parameterInliner.inline(string, parameterName, valueStr);
        }
        return string;
    }

    private List<String> getParametersToInsert(String string, Function<String, String> parameterNameUnwrapper) {
        List<String> parametersToInsert = new ArrayList<>();
        Matcher matcher = UNIVERSAL_ALIAS_PATTERN.matcher(string);
        while (matcher.find()) {
            String parameterName = parameterNameUnwrapper.apply(matcher.group());
            if (!ConditionMatcher.isConditionalMarker(parameterName)) {
                parametersToInsert.add(parameterName);
            }
        }
        return parametersToInsert;
    }

}
