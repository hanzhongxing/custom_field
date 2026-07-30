package com.customfield.service;

import com.customfield.model.CustomField;
import com.customfield.model.FieldRelation;
import com.customfield.util.FileStorageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomFieldService {

    @Autowired
    private FileStorageUtil fileStorageUtil;

    @Autowired
    private ExpressionService expressionService;

    public List<CustomField> getAllFields() {
        return fileStorageUtil.readCustomFields().stream()
                .sorted(Comparator.comparingInt(f -> f.getSortOrder() != null ? f.getSortOrder() : 999))
                .collect(Collectors.toList());
    }

    public CustomField getField(String key) {
        return getAllFields().stream()
                .filter(f -> f.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    public void saveOrUpdateField(CustomField field) {
        List<CustomField> fields = fileStorageUtil.readCustomFields();
        FieldRelation relation = fileStorageUtil.readFieldRelation();

        // 1. Update fields list
        boolean exists = false;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getKey().equals(field.getKey())) {
                fields.set(i, field);
                exists = true;
                break;
            }
        }
        if (!exists) {
            if (field.getSortOrder() == null) {
                field.setSortOrder(fields.size() + 1);
            }
            fields.add(field);
        }

        // 2. Extract dependencies from expression
        List<String> dependencies = expressionService.extractVariables(field.getExpression());
        
        // Only keep dependencies that are actual custom fields
        Set<String> validKeys = fields.stream().map(CustomField::getKey).collect(Collectors.toSet());
        dependencies = dependencies.stream().filter(validKeys::contains).collect(Collectors.toList());

        // Update relations graph map temporarily
        Map<String, List<String>> depMap = new HashMap<>(relation.getDependencies());
        depMap.put(field.getKey(), dependencies);

        // 3. Cycle Detection & Topological Sort
        List<String> sortedExecutionOrder = detectCycleAndSort(validKeys, depMap);

        // 4. Save updates
        relation.setDependencies(depMap);
        relation.setExecutionOrder(sortedExecutionOrder);
        
        fileStorageUtil.writeCustomFields(fields);
        fileStorageUtil.writeFieldRelation(relation);
    }

    public void deleteField(String key) {
        List<CustomField> fields = fileStorageUtil.readCustomFields();
        FieldRelation relation = fileStorageUtil.readFieldRelation();

        // check if others depend on this
        for (Map.Entry<String, List<String>> entry : relation.getDependencies().entrySet()) {
            if (entry.getValue().contains(key) && !entry.getKey().equals(key)) {
                throw new RuntimeException("Cannot delete field '" + key + "', it is referenced by '" + entry.getKey() + "'");
            }
        }

        fields.removeIf(f -> f.getKey().equals(key));
        relation.getDependencies().remove(key);
        relation.getExecutionOrder().remove(key);

        fileStorageUtil.writeCustomFields(fields);
        fileStorageUtil.writeFieldRelation(relation);
    }

    public void updateSortOrder(List<String> orderedKeys) {
        List<CustomField> fields = fileStorageUtil.readCustomFields();
        Map<String, CustomField> fieldMap = fields.stream().collect(Collectors.toMap(CustomField::getKey, f -> f));
        
        List<CustomField> updatedFields = new ArrayList<>();
        int order = 1;
        for (String key : orderedKeys) {
            CustomField f = fieldMap.get(key);
            if (f != null) {
                f.setSortOrder(order++);
                updatedFields.add(f);
            }
        }
        
        fileStorageUtil.writeCustomFields(updatedFields);
    }

    /**
     * DFS Cycle Detection and Topological Sort
     */
    private List<String> detectCycleAndSort(Set<String> allNodes, Map<String, List<String>> graph) {
        Map<String, Integer> state = new HashMap<>(); // 0: unvisited, 1: visiting, 2: visited
        for (String node : allNodes) {
            state.put(node, 0);
        }

        List<String> topoOrder = new ArrayList<>();
        List<String> currentPath = new ArrayList<>();

        for (String node : allNodes) {
            if (state.get(node) == 0) {
                if (dfs(node, graph, state, topoOrder, currentPath)) {
                    // format cycle message
                    int startIdx = currentPath.indexOf(currentPath.get(currentPath.size() - 1)); // find where cycle starts
                    List<String> cyclePath = currentPath.subList(startIdx, currentPath.size());
                    String cycleStr = String.join(" -> ", cyclePath);
                    throw new RuntimeException("存在循环引用 [" + cycleStr + "]，禁止保存！");
                }
            }
        }

        return topoOrder;
    }

    private boolean dfs(String node, Map<String, List<String>> graph, Map<String, Integer> state, List<String> topoOrder, List<String> currentPath) {
        state.put(node, 1); // visiting
        currentPath.add(node);

        List<String> neighbors = graph.getOrDefault(node, new ArrayList<>());
        for (String neighbor : neighbors) {
            if (!state.containsKey(neighbor)) continue; // ignore non-field variables
            
            if (state.get(neighbor) == 1) {
                // cycle detected
                currentPath.add(neighbor);
                return true;
            }
            if (state.get(neighbor) == 0) {
                if (dfs(neighbor, graph, state, topoOrder, currentPath)) {
                    return true;
                }
            }
        }

        state.put(node, 2); // visited
        currentPath.remove(currentPath.size() - 1);
        topoOrder.add(node); // post-order
        return false;
    }

    public Map<String, Object> evaluateAll(Map<String, Object> initialEnv) {
        List<CustomField> fields = fileStorageUtil.readCustomFields();
        Map<String, CustomField> fieldMap = fields.stream().collect(Collectors.toMap(CustomField::getKey, f -> f));
        FieldRelation relation = fileStorageUtil.readFieldRelation();
        
        Map<String, Object> env = new HashMap<>(initialEnv != null ? initialEnv : new HashMap<>());

        for (String key : relation.getExecutionOrder()) {
            CustomField field = fieldMap.get(key);
            if (field == null || "DISABLED".equals(field.getStatus())) {
                continue;
            }
            if (field.getExpression() != null && !field.getExpression().trim().isEmpty()) {
                try {
                    Object result = expressionService.dryRun(field.getExpression(), env);
                    env.put(key, result);
                } catch (Exception e) {
                    env.put(key, "Error: " + e.getMessage());
                }
            }
        }
        return env;
    }
}
