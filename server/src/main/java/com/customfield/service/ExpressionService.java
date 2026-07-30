package com.customfield.service;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpressionService {

    /**
     * Extracts variable names from an Aviator expression.
     * This is useful for finding which custom fields this expression depends on.
     */
    public List<String> extractVariables(String expressionStr) {
        if (expressionStr == null || expressionStr.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        try {
            Expression expression = AviatorEvaluator.compile(expressionStr);
            return expression.getVariableNames();
        } catch (Exception e) {
            throw new RuntimeException("Invalid expression: " + e.getMessage());
        }
    }

    /**
     * Executes the expression with the given environment map.
     */
    public Object dryRun(String expressionStr, Map<String, Object> env) {
        if (expressionStr == null || expressionStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            Expression expression = AviatorEvaluator.compile(expressionStr);
            return expression.execute(env);
        } catch (Exception e) {
            throw new RuntimeException("Execution error: " + e.getMessage());
        }
    }
}
