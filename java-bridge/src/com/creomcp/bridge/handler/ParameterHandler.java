package com.creomcp.bridge.handler;

import com.creomcp.bridge.CreoSessionManager;
import com.creomcp.bridge.util.JsonUtil;
import com.ptc.cipjava.jxthrowable;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcModelItem.ParamValue;
import com.ptc.pfc.pfcModelItem.ParamValueType;
import com.ptc.pfc.pfcModelItem.Parameter;
import com.ptc.pfc.pfcModelItem.Parameters;
import com.ptc.pfc.pfcModelItem.pfcModelItem;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/parameter/list, /api/parameter/get, /api/parameter/set, and /api/parameter/delete.
 */
public class ParameterHandler extends BaseHandler {

    private final String action;

    public ParameterHandler(String action) {
        this.action = action;
    }

    @Override
    protected Map<String, Object> execute(HttpExchange exchange) throws Exception {
        CreoSessionManager mgr = CreoSessionManager.getInstance();

        if ("list".equalsIgnoreCase(action)) {
            Map<String, String> query = getQueryParams(exchange);
            String modelName = query.get("model");
            String namePattern = query.get("pattern");

            Model model = mgr.resolveModel(modelName);
            Parameters params = model.ListParams();
            List<Map<String, Object>> resultList = new ArrayList<>();

            if (params != null) {
                int count = params.getarraysize();
                for (int i = 0; i < count; i++) {
                    Parameter p = params.get(i);
                    String name = p.GetName();
                    if (namePattern != null && !namePattern.isEmpty() && !matchesPattern(name, namePattern)) {
                        continue;
                    }
                    resultList.add(extractParamInfo(p));
                }
            }

            Map<String, Object> res = JsonUtil.createSuccess(null);
            res.put("model", model.GetFileName());
            res.put("count", resultList.size());
            res.put("parameters", resultList);
            return res;

        } else if ("get".equalsIgnoreCase(action)) {
            Map<String, String> query = getQueryParams(exchange);
            String name = query.get("name");
            String modelName = query.get("model");

            if (name == null || name.trim().isEmpty()) {
                return JsonUtil.createError("Missing query param 'name'");
            }

            Model model = mgr.resolveModel(modelName);
            Parameter p = model.GetParam(name);
            if (p == null) {
                return JsonUtil.createError("Parameter '" + name + "' not found on model " + model.GetFileName());
            }

            Map<String, Object> res = JsonUtil.createSuccess(null);
            res.put("model", model.GetFileName());
            res.put("parameter", extractParamInfo(p));
            return res;

        } else if ("set".equalsIgnoreCase(action)) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                return JsonUtil.createError("POST required for parameter set");
            }
            Map<String, Object> body = readJsonBody(exchange);
            String name = (String) body.get("name");
            Object value = body.get("value");
            String type = (String) body.get("type"); // STRING, DOUBLE, INTEGER, BOOLEAN
            String modelName = (String) body.get("model");
            String description = (String) body.get("description");

            if (name == null || name.trim().isEmpty()) {
                return JsonUtil.createError("Missing 'name' in request body.");
            }
            if (value == null) {
                return JsonUtil.createError("Missing 'value' in request body.");
            }

            Model model = mgr.resolveModel(modelName);
            Parameter p = model.GetParam(name);

            // Infer type if omitted
            if (type == null || type.trim().isEmpty()) {
                if (p != null) {
                    type = getTypeName(p.GetValue());
                } else {
                    if (value instanceof Number) {
                        type = (value instanceof Integer || value instanceof Long) ? "INTEGER" : "DOUBLE";
                    } else if (value instanceof Boolean) {
                        type = "BOOLEAN";
                    } else {
                        type = "STRING";
                    }
                }
            }

            ParamValue pval = createParamValue(value, type);

            if (p != null) {
                // Update existing
                p.SetValue(pval);
                if (description != null) {
                    p.SetDescription(description);
                }
            } else {
                // Create new parameter
                p = model.CreateParam(name, pval);
                if (description != null) {
                    p.SetDescription(description);
                }
            }

            Map<String, Object> res = JsonUtil.createSuccess("Parameter '" + name + "' set successfully on " + model.GetFileName());
            res.put("model", model.GetFileName());
            res.put("parameter", extractParamInfo(p));
            return res;

        } else if ("delete".equalsIgnoreCase(action)) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                return JsonUtil.createError("POST required for parameter delete");
            }
            Map<String, Object> body = readJsonBody(exchange);
            String name = (String) body.get("name");
            String modelName = (String) body.get("model");

            if (name == null || name.trim().isEmpty()) {
                return JsonUtil.createError("Missing 'name' in request body.");
            }

            Model model = mgr.resolveModel(modelName);
            Parameter p = model.GetParam(name);
            if (p == null) {
                return JsonUtil.createError("Parameter '" + name + "' does not exist on model " + model.GetFileName());
            }

            p.Delete();
            return JsonUtil.createSuccess("Parameter '" + name + "' deleted from " + model.GetFileName());

        } else {
            return JsonUtil.createError("Unknown parameter action: " + action);
        }
    }

    private Map<String, Object> extractParamInfo(Parameter p) throws jxthrowable {
        Map<String, Object> info = new HashMap<>();
        info.put("name", p.GetName());
        info.put("isDesignated", p.GetIsDesignated());
        info.put("isRelationDriven", p.GetIsRelationDriven());

        try {
            info.put("description", p.GetDescription());
        } catch (Exception ignored) {}

        ParamValue val = p.GetValue();
        if (val != null) {
            info.put("type", getTypeName(val));
            info.put("value", getRawValue(val));
        } else {
            info.put("type", "UNKNOWN");
            info.put("value", null);
        }
        return info;
    }

    private String getTypeName(ParamValue val) throws jxthrowable {
        ParamValueType discr = val.Getdiscr();
        int v = discr.getValue();
        if (v == ParamValueType._PARAM_STRING) return "STRING";
        if (v == ParamValueType._PARAM_DOUBLE) return "DOUBLE";
        if (v == ParamValueType._PARAM_INTEGER) return "INTEGER";
        if (v == ParamValueType._PARAM_BOOLEAN) return "BOOLEAN";
        if (v == ParamValueType._PARAM_NOTE) return "NOTE";
        return "OTHER";
    }

    private Object getRawValue(ParamValue val) throws jxthrowable {
        ParamValueType discr = val.Getdiscr();
        int v = discr.getValue();
        if (v == ParamValueType._PARAM_STRING) return val.GetStringValue();
        if (v == ParamValueType._PARAM_DOUBLE) return val.GetDoubleValue();
        if (v == ParamValueType._PARAM_INTEGER) return val.GetIntValue();
        if (v == ParamValueType._PARAM_BOOLEAN) return val.GetBoolValue();
        if (v == ParamValueType._PARAM_NOTE) return val.GetNoteId();
        return null;
    }

    private ParamValue createParamValue(Object val, String type) throws jxthrowable {
        String t = type.toUpperCase();
        if ("STRING".equals(t)) {
            return pfcModelItem.CreateStringParamValue(val.toString());
        } else if ("DOUBLE".equals(t) || "FLOAT".equals(t) || "NUMBER".equals(t)) {
            double d = Double.parseDouble(val.toString());
            return pfcModelItem.CreateDoubleParamValue(d);
        } else if ("INTEGER".equals(t) || "INT".equals(t)) {
            int i = (int) Math.round(Double.parseDouble(val.toString()));
            return pfcModelItem.CreateIntParamValue(i);
        } else if ("BOOLEAN".equals(t) || "BOOL".equals(t)) {
            boolean b = Boolean.parseBoolean(val.toString());
            return pfcModelItem.CreateBoolParamValue(b);
        } else {
            return pfcModelItem.CreateStringParamValue(val.toString());
        }
    }

    private boolean matchesPattern(String str, String pattern) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
        return str.matches("(?i)" + regex);
    }
}

