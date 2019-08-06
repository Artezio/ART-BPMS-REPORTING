package com.artezio.reporting.yarg.formatters.conditional;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConditionMatcherTest {

    private final String textPrefix = "Text before IF blocks";
    private final String textSuffix = "Text after IF blocks";
    private final String textInIfBlock = "Some text in IF block";
    private final String textInElseBlock = "Some other text in Else block";
    private final String textWihtoutMatches = "There are no IF blocks in this text ${if.property} ${ -if } ${iffi } ${If wrong case} ${ELSE} ${ENDIF}";

    private final String textWithIfEndifMatch = textPrefix + "${IF someCondition}" + textInIfBlock + "${ENDIF}" + textSuffix;
    private final String textWithIfElseEndifMatch = textPrefix + "${IF someCondition}" + textInIfBlock + "${ELSE}" + textInElseBlock + "${ENDIF}" + textSuffix;

    @Test
    public void testMatches() {
        ConditionMatcher matcher = new ConditionMatcher();

        assertFalse(matcher.match(textWihtoutMatches));
        assertTrue(matcher.match(textWithIfEndifMatch));
        assertTrue(matcher.match(textWithIfElseEndifMatch));
    }

    @Test
    public void testGetResultText() {
        ConditionMatcher matcher = new ConditionMatcher();
        matcher.match(textWithIfEndifMatch);
        assertEquals(textPrefix + textInIfBlock + textSuffix, matcher.getResultText(true));
        assertEquals("", matcher.getResultText(false));
        matcher.match(textWithIfElseEndifMatch);
        assertEquals(textPrefix + textInIfBlock + textSuffix, matcher.getResultText(true));
        assertEquals(textPrefix + textInElseBlock + textSuffix, matcher.getResultText(false));
    }
}
