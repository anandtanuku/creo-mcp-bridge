package com.creomcp.bridge.handler;

import com.creomcp.bridge.CreoSessionManager;
import com.creomcp.bridge.util.JsonUtil;
import com.ptc.cipjava.jxthrowable;
import com.ptc.pfc.pfcFeature.Feature;
import com.ptc.pfc.pfcFeature.Features;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcSolid.Solid;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/feature/list.
 */
public class FeatureHandler extends BaseHandler {

    private final String action;

    public FeatureHandler(String action) {
        this.action = action;
    }

    @Override
    protected Map<String, Object> execute(HttpExchange exchange) throws Exception {
        CreoSessionManager mgr = CreoSessionManager.getInstance();

        if ("list".equalsIgnoreCase(action)) {
            Map<String, String> query = getQueryParams(exchange);
            String modelName = query.get("model");
            String typeFilter = query.get("type");

            Model model = mgr.resolveModel(modelName);
            if (!(model instanceof Solid)) {
                return JsonUtil.createError("Model '" + model.GetFileName() + "' is not a Solid model (no features).");
            }

            Solid solid = (Solid) model;
            Features feats = solid.ListFeaturesByType(null, null);
            List<Map<String, Object>> resultList = new ArrayList<>();

            if (feats != null) {
                int count = feats.getarraysize();
                for (int i = 0; i < count; i++) {
                    Feature f = feats.get(i);
                    String featTypeName = f.GetFeatTypeName();
                    if (typeFilter != null && !typeFilter.isEmpty() && !typeFilter.equalsIgnoreCase(featTypeName)) {
                        continue;
                    }
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", f.GetId());
                    item.put("name", f.GetName());
                    item.put("typeName", featTypeName);
                    item.put("status", f.GetStatus().toString());
                    item.put("isVisible", f.GetIsVisible());
                    item.put("isReadonly", f.GetIsReadonly());
                    try {
                        item.put("number", f.GetNumber());
                    } catch (Exception ignored) {}
                    resultList.add(item);
                }
            }

            Map<String, Object> res = JsonUtil.createSuccess(null);
            res.put("model", model.GetFileName());
            res.put("count", resultList.size());
            res.put("features", resultList);
            return res;

        } else {
            return JsonUtil.createError("Unknown feature action: " + action);
        }
    }
}

