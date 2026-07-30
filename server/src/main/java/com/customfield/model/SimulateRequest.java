package com.customfield.model;

import lombok.Data;
import java.util.Map;

@Data
public class SimulateRequest {
    private String expression;
    private Map<String, Object> env;
}
