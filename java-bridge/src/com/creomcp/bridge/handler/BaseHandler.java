package com.creomcp.bridge.handler;

import com.creomcp.bridge.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Base HTTP handler providing JSON parsing, response writing, CORS, and query parameter extraction.
 */
public abstract class BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            Map<String, Object> response = execute(exchange);
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = JsonUtil.createError(e.getMessage());
            sendJsonResponse(exchange, 500, err);
        }
    }

    protected abstract Map<String, Object> execute(HttpExchange exchange) throws Exception;

    protected Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int len;
        while ((len = is.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        String body = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        return JsonUtil.parseMap(body);
    }

    protected Map<String, String> getQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    params.put(pair.substring(0, idx), pair.substring(idx + 1));
                } else {
                    params.put(pair, "");
                }
            }
        }
        return params;
    }

    protected void sendJsonResponse(HttpExchange exchange, int statusCode, Map<String, Object> response) throws IOException {
        String json = JsonUtil.toJson(response);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}

