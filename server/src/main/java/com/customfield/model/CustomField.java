package com.customfield.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomField {
    private String key;
    private String name;
    private String description;
    
    /**
     * NUMERIC, TEXT, DATE
     */
    private String type;
    
    private String expression;
    
    /**
     * ENABLED, DISABLED
     */
    private String status;
    
    private Integer sortOrder;
}
