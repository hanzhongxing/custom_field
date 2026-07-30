package com.customfield.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldRelation {
    // Map of field key -> list of field keys that this field depends on
    // e.g., "field_b" -> ["field_a"] means field_b uses field_a in its expression
    private Map<String, List<String>> dependencies = new HashMap<>();
    
    // Ordered list of field keys defining the execution topological sort order
    private List<String> executionOrder = new ArrayList<>();
}
