package com.creomcp.bridge.handler;

import com.creomcp.bridge.CreoSessionManager;
import com.creomcp.bridge.util.JsonUtil;
import com.ptc.cipjava.jxthrowable;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcModel.ModelDescriptor;
import com.ptc.pfc.pfcModel.Models;
import com.ptc.pfc.pfcModel.pfcModel;
import com.ptc.pfc.pfcSession.Session;
import com.ptc.pfc.pfcSolid.Solid;
import com.ptc.pfc.pfcWindow.Window;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/model/active, /api/model/list, /api/model/open, /api/model/save, and /api/model/regenerate.
 */
public class ModelHandler extends BaseHandler {

    private final String action;

    public ModelHandler(String action) {
        this.action = action;
    }

    @Override
    protected Map<String, Object> execute(HttpExchange exchange) throws Exception {
        CreoSessionManager mgr = CreoSessionManager.getInstance();
        Session session = mgr.getSession();

        if ("active".equalsIgnoreCase(action)) {
            Model model = mgr.getActiveModel();
            if (model == null) {
                Map<String, Object> res = JsonUtil.createSuccess("No model currently active.");
                res.put("model", null);
                return res;
            }
            Map<String, Object> res = JsonUtil.createSuccess(null);
            res.put("model", extractModelInfo(model));
            return res;

        } else if ("list".equalsIgnoreCase(action)) {
            Models models = session.ListModels();
            List<Map<String, Object>> list = new ArrayList<>();
            if (models != null) {
                int count = models.getarraysize();
                for (int i = 0; i < count; i++) {
                    list.add(extractModelInfo(models.get(i)));
                }
            }
            Map<String, Object> res = JsonUtil.createSuccess(null);
            res.put("count", list.size());
            res.put("models", list);
            return res;

        } else if ("open".equalsIgnoreCase(action)) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                return JsonUtil.createError("POST required for open model");
            }
            Map<String, Object> body = readJsonBody(exchange);
            String file = (String) body.get("file");
            if (file == null || file.trim().isEmpty()) {
                return JsonUtil.createError("Missing 'file' property in request body.");
            }

            boolean display = body.containsKey("display") ? Boolean.parseBoolean(body.get("display").toString()) : true;
            boolean activate = body.containsKey("activate") ? Boolean.parseBoolean(body.get("activate").toString()) : true;

            try {
                // If full path provided, switch directory if needed or load directly
                File f = new File(file);
                if (f.isAbsolute() && f.getParent() != null) {
                    try {
                        session.ChangeDirectory(f.getParent());
                    } catch (Exception ignored) {}
                    file = f.getName();
                }

                ModelDescriptor desc = pfcModel.ModelDescriptor_CreateFromFileName(file);
                Model model = session.RetrieveModel(desc);
                if (model == null) {
                    return JsonUtil.createError("Failed to retrieve model: " + file);
                }

                if (display) {
                    model.Display();
                    Window win = session.GetModelWindow(model);
                    if (win != null && activate) {
                        win.Activate();
                    }
                }

                Map<String, Object> res = JsonUtil.createSuccess("Model opened successfully: " + file);
                res.put("model", extractModelInfo(model));
                return res;

            } catch (jxthrowable jxe) {
                return JsonUtil.createError("Creo open error: " + CreoSessionManager.formatCipException(jxe));
            }

        } else if ("save".equalsIgnoreCase(action)) {
            Map<String, Object> body = "POST".equalsIgnoreCase(exchange.getRequestMethod()) ? readJsonBody(exchange) : new HashMap<>();
            String modelName = (String) body.get("model");
            Model model = mgr.resolveModel(modelName);

            try {
                model.Save();
                Map<String, Object> res = JsonUtil.createSuccess("Model saved successfully: " + model.GetFileName());
                res.put("model", extractModelInfo(model));
                return res;
            } catch (jxthrowable jxe) {
                return JsonUtil.createError("Creo save error: " + CreoSessionManager.formatCipException(jxe));
            }

        } else if ("regenerate".equalsIgnoreCase(action)) {
            Map<String, Object> body = "POST".equalsIgnoreCase(exchange.getRequestMethod()) ? readJsonBody(exchange) : new HashMap<>();
            String modelName = (String) body.get("model");
            Model model = mgr.resolveModel(modelName);

            if (model instanceof Solid) {
                try {
                    Solid solid = (Solid) model;
                    solid.Regenerate(null);
                    model.Display(); // Refresh graphics view

                    Map<String, Object> res = JsonUtil.createSuccess("Model regenerated successfully: " + model.GetFileName());
                    res.put("model", extractModelInfo(model));
                    return res;
                } catch (jxthrowable jxe) {
                    return JsonUtil.createError("Creo regeneration error: " + CreoSessionManager.formatCipException(jxe));
                }
            } else {
                return JsonUtil.createError("Model '" + model.GetFileName() + "' is not a Solid model (cannot regenerate).");
            }

        } else {
            return JsonUtil.createError("Unknown model action: " + action);
        }
    }

    private Map<String, Object> extractModelInfo(Model m) {
        Map<String, Object> info = new HashMap<>();
        try {
            info.put("filename", m.GetFileName());
            info.put("commonName", m.GetCommonName());
            info.put("type", m.GetType().toString());
            info.put("isModified", m.GetIsModified());
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }
}

