package com.creomcp.bridge;

import com.creomcp.bridge.handler.BaseHandler;
import com.creomcp.bridge.handler.ConnectionHandler;
import com.creomcp.bridge.handler.DrawingHandler;
import com.creomcp.bridge.handler.ExportHandler;
import com.creomcp.bridge.handler.FeatureHandler;
import com.creomcp.bridge.handler.ModelHandler;
import com.creomcp.bridge.handler.ParameterHandler;
import com.creomcp.bridge.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Main entry point for the standalone Creo MCP Java Bridge HTTP server.
 */
public class MainBridgeServer {

    public static final int DEFAULT_PORT = 9090;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port specified: " + args[0] + ", using default: " + DEFAULT_PORT);
            }
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

            // Root / Health info
            server.createContext("/", new BaseHandler() {
                @Override
                protected Map<String, Object> execute(HttpExchange exchange) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("service", "PTC Creo Parametric AI Java Bridge");
                    res.put("version", "1.0.0");
                    res.put("status", "running");
                    res.put("connectedToCreo", CreoSessionManager.getInstance().isConnected());
                    return res;
                }
            });

            // Connection Endpoints
            server.createContext("/api/status", new ConnectionHandler("status"));
            server.createContext("/api/connect", new ConnectionHandler("connect"));
            server.createContext("/api/disconnect", new ConnectionHandler("disconnect"));

            // Model Endpoints
            server.createContext("/api/model/active", new ModelHandler("active"));
            server.createContext("/api/model/list", new ModelHandler("list"));
            server.createContext("/api/model/open", new ModelHandler("open"));
            server.createContext("/api/model/save", new ModelHandler("save"));
            server.createContext("/api/model/regenerate", new ModelHandler("regenerate"));

            // Parameter Endpoints
            server.createContext("/api/parameter/list", new ParameterHandler("list"));
            server.createContext("/api/parameter/get", new ParameterHandler("get"));
            server.createContext("/api/parameter/set", new ParameterHandler("set"));
            server.createContext("/api/parameter/delete", new ParameterHandler("delete"));

            // Feature Endpoints
            server.createContext("/api/feature/list", new FeatureHandler("list"));

            // Drawing Endpoints
            server.createContext("/api/drawing/create", new DrawingHandler("create"));
            server.createContext("/api/drawing/show_dimensions", new DrawingHandler("show_dimensions"));


            // Export Endpoints
            server.createContext("/api/export/step", new ExportHandler("step"));
            server.createContext("/api/export/pdf", new ExportHandler("pdf"));

            // Shutdown Endpoint
            server.createContext("/api/shutdown", new BaseHandler() {
                @Override
                protected Map<String, Object> execute(HttpExchange exchange) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            server.stop(0);
                            System.out.println("[MainBridgeServer] Bridge server stopped.");
                            System.exit(0);
                        } catch (Exception ignored) {}
                    }).start();
                    return JsonUtil.createSuccess("Shutting down bridge server...");
                }
            });

            // Use multi-threaded executor for handling concurrent requests
            server.setExecutor(Executors.newFixedThreadPool(4));

            System.out.println("==========================================================");
            System.out.println("   Creo MCP Java Bridge Server started successfully!");
            System.out.println("   Listening on: http://127.0.0.1:" + port);
            System.out.println("==========================================================");

            server.start();

            // Try initial connection in background
            new Thread(() -> {
                try {
                    System.out.println("[MainBridgeServer] Initializing connection to Creo...");
                    CreoSessionManager.getInstance().connect(10);
                } catch (Exception e) {
                    System.out.println("[MainBridgeServer] Creo initial connect check: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("Failed to start bridge server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

