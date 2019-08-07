package com.artezio.bpm.reporting.yarg.formatters.el;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ELEvaluatorTest {

    @Test
    public void testHasElExpression() {
        assertTrue(ELEvaluator.hasElExpression("#{true} some text with expressions #{5+2}"));
        assertFalse(ELEvaluator.hasElExpression("# {} some text without #{expressions"));
    }

    @Test
    public void testHasCompleteExpression() {
        assertTrue(ELEvaluator.hasCompleteExpression("#{bean.method(${band.data})}"));
        assertFalse(ELEvaluator.hasCompleteExpression("#{bean.method(${band.data})"));
        assertTrue(ELEvaluator.hasCompleteExpression("${IF #{bean.method(${band.data})}"));
        assertTrue(ELEvaluator.hasCompleteExpression("#{a ? b : ${band.data})} }"));
    }
}
