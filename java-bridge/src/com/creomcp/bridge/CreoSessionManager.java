package com.creomcp.bridge;

import com.ptc.cipjava.jxthrowable;
import com.ptc.pfc.pfcAsyncConnection.AsyncConnection;
import com.ptc.pfc.pfcAsyncConnection.pfcAsyncConnection;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcModel.Models;
import com.ptc.pfc.pfcSession.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the connection lifecycle and interaction with PTC Creo Parametric via J-Link.
 */
public class CreoSessionManager {

    private static final CreoSessionManager instance = new CreoSessionManager();

    private AsyncConnection asyncConnection = null;
    private Session session = null;
    private boolean libraryLoaded = false;

    private CreoSessionManager() {
    }

    public static CreoSessionManager getInstance() {
        return instance;
    }

    /**
     * Loads the native pfcasyncmt library from Creo.
     */
    public synchronized void ensureLibraryLoaded() {
        if (!libraryLoaded) {
            try {
                System.loadLibrary("pfcasyncmt");
                libraryLoaded = true;
                System.out.println("[CreoSessionManager] J-Link native library 'pfcasyncmt' loaded successfully.");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("[CreoSessionManager] Failed to load 'pfcasyncmt': " + e.getMessage());
                throw new RuntimeException("Could not load Creo native library 'pfcasyncmt'. Ensure Creo lib/obj directories are in PATH.", e);
            }
        }
    }

    /**
     * Connects to a running instance of Creo Parametric.
     * @param timeoutSeconds Connection timeout in seconds (default 30)
     */
    public synchronized Map<String, Object> connect(int timeoutSeconds) {
        ensureLibraryLoaded();

        if (isConnected()) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Already connected to Creo.");
            res.put("directory", getCurrentDirectory());
            return res;
        }

        try {
            System.out.println("[CreoSessionManager] Attempting to connect to Creo (timeout: " + timeoutSeconds + "s)...");
            asyncConnection = pfcAsyncConnection.AsyncConnection_Connect(null, null, null, timeoutSeconds);
            if (asyncConnection == null) {
                return errorResult("AsyncConnection_Connect returned null. Is Creo running?");
            }

            session = asyncConnection.GetSession();
            if (session == null) {
                return errorResult("Connected, but failed to retrieve Creo Session object.");
            }

            String dir = session.GetCurrentDirectory();
            System.out.println("[CreoSessionManager] Successfully connected to Creo session! Working dir: " + dir);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Connected to Creo Parametric successfully.");
            result.put("directory", dir);
            result.put("connectionId", session.GetConnectionId());
            return result;

        } catch (jxthrowable jxe) {
            System.err.println("[CreoSessionManager] J-Link connect error: " + jxe.getMessage());
            return errorResult("Creo connection error: " + formatCipException(jxe));
        } catch (Exception e) {
            System.err.println("[CreoSessionManager] Unexpected connect error: " + e.getMessage());
            return errorResult("Connection failed: " + e.getMessage());
        }
    }

    /**
     * Disconnects from Creo.
     */
    public synchronized Map<String, Object> disconnect() {
        if (!isConnected()) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Not currently connected.");
            return res;
        }

        try {
            if (asyncConnection != null) {
                asyncConnection.Disconnect(2);
            }
            asyncConnection = null;
            session = null;
            System.out.println("[CreoSessionManager] Disconnected from Creo.");

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Disconnected successfully.");
            return res;
        } catch (Exception e) {
            asyncConnection = null;
            session = null;
            return errorResult("Error while disconnecting: " + e.getMessage());
        }
    }

    /**
     * Checks if connection to Creo is currently active.
     */
    public synchronized boolean isConnected() {
        if (asyncConnection == null || session == null) {
            return false;
        }
        try {
            return asyncConnection.IsRunning();
        } catch (Exception e) {
            asyncConnection = null;
            session = null;
            return false;
        }
    }

    /**
     * Returns the active Session object, throwing if not connected.
     */
    public synchronized Session getSession() throws Exception {
        if (!isConnected()) {
            // Attempt auto-connect once with short timeout
            Map<String, Object> connRes = connect(5);
            if (!isConnected()) {
                throw new IllegalStateException("Not connected to Creo. Please call /api/connect first. Details: " + connRes.get("message"));
            }
        }
        return session;
    }

    /**
     * Returns the active CAD model, or null if no model is open.
     */
    public synchronized Model getActiveModel() throws Exception {
        Session s = getSession();
        try {
            return s.GetCurrentModel();
        } catch (jxthrowable e) {
            return null;
        }
    }

    /**
     * Retrieves a loaded model by name, or falls back to active model if name is omitted.
     */
    public synchronized Model resolveModel(String modelName) throws Exception {
        Session s = getSession();
        if (modelName == null || modelName.trim().isEmpty()) {
            Model m = s.GetCurrentModel();
            if (m == null) {
                throw new IllegalArgumentException("No active model in Creo session, and no model name was specified.");
            }
            return m;
        }

        Models models = s.ListModels();
        if (models != null) {
            int count = models.getarraysize();
            for (int i = 0; i < count; i++) {
                Model m = models.get(i);
                if (m.GetFileName().equalsIgnoreCase(modelName) || m.GetFullName().equalsIgnoreCase(modelName)) {
                    return m;
                }
            }
        }

        // Try getting by filename directly
        try {
            Model m = s.GetModelFromFileName(modelName);
            if (m != null) return m;
        } catch (Exception ignored) {}

        throw new IllegalArgumentException("Model '" + modelName + "' is not currently loaded in Creo.");
    }

    /**
     * Returns current Creo working directory.
     */
    public String getCurrentDirectory() {
        if (session == null) return null;
        try {
            return session.GetCurrentDirectory();
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatCipException(jxthrowable jxe) {
        if (jxe == null) return "Unknown J-Link error";
        String msg = jxe.getMessage();
        String className = jxe.getClass().getSimpleName();
        if (msg == null || msg.trim().isEmpty()) {
            return className;
        }
        return className + ": " + msg;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("error", true);
        err.put("message", message);
        return err;
    }
}

