package com.customfield.controller;

import com.customfield.model.CustomField;
import com.customfield.model.Result;
import com.customfield.model.SimulateRequest;
import com.customfield.service.CustomFieldService;
import com.customfield.service.ExpressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fields")
public class CustomFieldController {

    @Autowired
    private CustomFieldService customFieldService;
    
    @Autowired
    private ExpressionService expressionService;

    @GetMapping
    public Result<List<CustomField>> listFields() {
        return Result.success(customFieldService.getAllFields());
    }

    @PostMapping
    public Result<Void> saveField(@RequestBody CustomField field) {
        try {
            customFieldService.saveOrUpdateField(field);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @DeleteMapping("/{key}")
    public Result<Void> deleteField(@PathVariable String key) {
        try {
            customFieldService.deleteField(key);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PutMapping("/sort")
    public Result<Void> updateSortOrder(@RequestBody List<String> orderedKeys) {
        try {
            customFieldService.updateSortOrder(orderedKeys);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/simulate")
    public Result<Object> simulateExpression(@RequestBody SimulateRequest request) {
        try {
            Object result = expressionService.dryRun(request.getExpression(), request.getEnv());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @PostMapping("/evaluateAll")
    public Result<Map<String, Object>> evaluateAll(@RequestBody Map<String, Object> env) {
        try {
            return Result.success(customFieldService.evaluateAll(env));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
