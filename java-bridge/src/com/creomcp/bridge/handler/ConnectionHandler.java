package com.creomcp.bridge.handler;

import com.creomcp.bridge.CreoSessionManager;
import com.creomcp.bridge.util.JsonUtil;
import com.ptc.pfc.pfcModel.Model;
import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles /api/status, /api/connect, and /api/disconnect.
 */
public class ConnectionHandler extends BaseHandler {

    private final String action;

    public ConnectionHandler(String action) {
        this.action = action;
    }

    @Override
    protected Map<String, Object> execute(HttpExchange exchange) throws Exception {
        CreoSessionManager mgr = CreoSessionManager.getInstance();

        if ("connect".equalsIgnoreCase(action)) {
            int timeout = 30;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, Object> body = readJsonBody(exchange);
                if (body.containsKey("timeout")) {
                    try {
                        timeout = Integer.parseInt(body.get("timeout").toString());
                    } catch (NumberFormatException ignored) {}
                }
            }
            return mgr.connect(timeout);

        } else if ("disconnect".equalsIgnoreCase(action)) {
            return mgr.disconnect();

        } else if ("status".equalsIgnoreCase(action)) {
            Map<String, Object> res = new HashMap<>();
            boolean connected = mgr.isConnected();
            res.put("success", true);
            res.put("connected", connected);

            if (connected) {
                res.put("directory", mgr.getCurrentDirectory());
                try {
                    Model active = mgr.getActiveModel();
                    if (active != null) {
                        Map<String, Object> modelInfo = new HashMap<>();
                        modelInfo.put("filename", active.GetFileName());
                        modelInfo.put("commonName", active.GetCommonName());
                        modelInfo.put("type", active.GetType().toString());
                        modelInfo.put("isModified", active.GetIsModified());
                        res.put("activeModel", modelInfo);
                    } else {
                        res.put("activeModel", null);
                    }
                } catch (Exception e) {
                    res.put("activeModel", null);
                }
            } else {
                res.put("directory", null);
                res.put("activeModel", null);
            }
            return res;

        } else {
            return JsonUtil.createError("Unknown connection action: " + action);
        }
    }
}

