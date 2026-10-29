package com.creomcp.bridge.handler;

import com.creomcp.bridge.CreoSessionManager;
import com.creomcp.bridge.util.JsonUtil;
import com.ptc.cipjava.jxthrowable;
import com.ptc.pfc.pfcAssembly.ComponentPath;
import com.ptc.pfc.pfcDimension.BaseDimension;
import com.ptc.pfc.pfcDrawing.Drawing;
import com.ptc.pfc.pfcDrawing.DrawingCreateOption;
import com.ptc.pfc.pfcDrawing.DrawingCreateOptions;
import com.ptc.pfc.pfcFeature.Feature;
import com.ptc.pfc.pfcFeature.Features;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcModel.ModelDescriptor;
import com.ptc.pfc.pfcModel2D.Model2D;
import com.ptc.pfc.pfcModelItem.ModelItem;
import com.ptc.pfc.pfcModelItem.ModelItemType;
import com.ptc.pfc.pfcModelItem.ModelItems;
import com.ptc.pfc.pfcSession.Session;
import com.ptc.pfc.pfcSolid.Solid;
import com.ptc.pfc.pfcView2D.DrawingDimensionShowInstructions;
import com.ptc.pfc.pfcView2D.View2D;
import com.ptc.pfc.pfcView2D.View2Ds;
import com.ptc.pfc.pfcView2D.pfcView2D;
import com.ptc.pfc.pfcWindow.Window;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.util.*;

/**
 * Handles /api/drawing/create, /api/drawing/show_dimensions, and drawing operations.
 */
public class DrawingHandler extends BaseHandler {

    private final String action;

    public DrawingHandler(String action) {
        this.action = action;
    }

    @Override
    protected Map<String, Object> execute(HttpExchange exchange) throws Exception {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return JsonUtil.createError("POST required for drawing operations");
        }

        CreoSessionManager mgr = CreoSessionManager.getInstance();
        Session session = mgr.getSession();
        Map<String, Object> body = readJsonBody(exchange);

        if ("create".equalsIgnoreCase(action)) {
            String modelName = (String) body.get("model");
            String drawingName = (String) body.get("drawing");
            String template = (String) body.get("template");
            boolean display = body.containsKey("display") ? Boolean.parseBoolean(body.get("display").toString()) : true;
            boolean activate = body.containsKey("activate") ? Boolean.parseBoolean(body.get("activate").toString()) : true;
            boolean showDimensions = body.containsKey("show_dimensions") ? Boolean.parseBoolean(body.get("show_dimensions").toString()) : true;

            // Resolve target 3D model
            Model model = mgr.resolveModel(modelName);
            ModelDescriptor modelDesc = model.GetDescr();

            // Default drawing name to <modelName>_drw if not specified
            if (drawingName == null || drawingName.trim().isEmpty()) {
                String base = model.GetFileName();
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                drawingName = base + "_drw";
            }
            if (drawingName.toLowerCase().endsWith(".drw")) {
                drawingName = drawingName.substring(0, drawingName.length() - 4);
            }

            // Default template to c_drawing if omitted
            if (template == null || template.trim().isEmpty()) {
                template = "c_drawing";
            }

            // Ensure template file is available in working directory and get short template name
            String templateName = prepareTemplate(template, mgr.getCurrentDirectory());

            try {
                DrawingCreateOptions opts = DrawingCreateOptions.create();
                opts.append(DrawingCreateOption.DRAWINGCREATE_SHOW_ERROR_DIALOG);

                System.out.println("[DrawingHandler] Creating drawing '" + drawingName + "' with template '" + templateName + "' for model '" + model.GetFileName() + "'...");
                
                Drawing drawing = null;
                try {
                    drawing = session.CreateDrawingFromTemplate(drawingName, templateName, modelDesc, opts);
                } catch (jxthrowable jxe) {
                    // If not found without extension, try with .drw or vice versa
                    String altName = templateName.toLowerCase().endsWith(".drw") 
                            ? templateName.substring(0, templateName.length() - 4) 
                            : (templateName + ".drw");
                    System.out.println("[DrawingHandler] Retrying with alt template name: " + altName);
                    drawing = session.CreateDrawingFromTemplate(drawingName, altName, modelDesc, opts);
                }

                if (drawing == null) {
                    return JsonUtil.createError("Failed to create drawing from template: " + templateName);
                }

                if (display) {
                    drawing.Display();
                    Window win = session.GetModelWindow(drawing);
                    if (win != null && activate) {
                        win.Activate();
                    }
                }

                // Show dimensions automatically if requested
                int dimsShown = 0;
                if (showDimensions) {
                    dimsShown = showModelDimensionsOnDrawing(drawing, model, session);
                }

                // Regenerate drawing to ensure all views, dimensions, and title block parameters update
                drawing.Regenerate();
                Window win = session.GetModelWindow(drawing);
                if (win != null) {
                    try {
                        win.Refresh();
                        win.Repaint();
                    } catch (Exception ignored) {}
                }

                Map<String, Object> res = JsonUtil.createSuccess("Drawing created successfully: " + drawing.GetFileName());
                res.put("drawingName", drawing.GetFileName());
                res.put("commonName", drawing.GetCommonName());
                res.put("model", model.GetFileName());
                res.put("templateUsed", templateName);
                res.put("dimensionsShown", dimsShown);
                return res;

            } catch (jxthrowable jxe) {
                return JsonUtil.createError("Drawing creation error: " + CreoSessionManager.formatCipException(jxe));
            }

        } else if ("show_dimensions".equalsIgnoreCase(action)) {
            String drawingName = (String) body.get("drawing");
            String modelName = (String) body.get("model");

            Model targetModel = mgr.resolveModel(drawingName);
            if (!(targetModel instanceof Drawing)) {
                return JsonUtil.createError("Target model '" + targetModel.GetFileName() + "' is not a Drawing (.drw)");
            }
            Drawing drawing = (Drawing) targetModel;

            Model solidModel = null;
            if (modelName != null && !modelName.trim().isEmpty()) {
                solidModel = mgr.resolveModel(modelName);
            } else {
                try {
                    solidModel = ((Model2D) drawing).GetCurrentSolid();
                } catch (Exception ignored) {}
                if (solidModel == null) {
                    return JsonUtil.createError("Could not determine underlying 3D model for drawing. Please specify 'model'.");
                }
            }

            int dimsShown = showModelDimensionsOnDrawing(drawing, solidModel, session);
            drawing.Regenerate();
            Window win = session.GetModelWindow(drawing);
            if (win != null) {
                try {
                    win.Refresh();
                    win.Repaint();
                } catch (Exception ignored) {}
            }

            Map<String, Object> res = JsonUtil.createSuccess("Displayed " + dimsShown + " dimensions on drawing: " + drawing.GetFileName());
            res.put("drawing", drawing.GetFileName());
            res.put("model", solidModel.GetFileName());
            res.put("dimensionsShown", dimsShown);
            return res;

        } else {
            return JsonUtil.createError("Unknown drawing action: " + action);
        }
    }

    /**
     * Finds and displays model annotations/dimensions on 2D drawing views.
     */
    public static int showModelDimensionsOnDrawing(Drawing drawing, Model model, Session session) {
        int shownCount = 0;
        try {
            View2Ds views = drawing.List2DViews();
            int viewCount = (views != null) ? views.getarraysize() : 0;
            if (viewCount == 0) {
                System.out.println("[DrawingHandler] No 2D views found on drawing " + drawing.GetFileName());
                return 0;
            }

            System.out.println("[DrawingHandler] Drawing has " + viewCount + " views.");
            for (int v = 0; v < viewCount; v++) {
                System.out.println("  View " + v + ": " + views.get(v).GetName());
            }

            // Collect all unique dimensions
            Map<Integer, BaseDimension> dimsMap = new LinkedHashMap<>();

            // 1. Top-level model dimensions
            try {
                ModelItems items = model.ListItems(ModelItemType.ITEM_DIMENSION);
                if (items != null) {
                    for (int i = 0; i < items.getarraysize(); i++) {
                        ModelItem item = items.get(i);
                        if (item instanceof BaseDimension) {
                            dimsMap.put(item.GetId(), (BaseDimension) item);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[DrawingHandler] Model.ListItems(ITEM_DIMENSION): " + e.getMessage());
            }

            // 2. Feature-level sub-items
            if (model instanceof Solid) {
                try {
                    Features feats = ((Solid) model).ListFeaturesByType(Boolean.TRUE, null);
                    if (feats != null) {
                        for (int f = 0; f < feats.getarraysize(); f++) {
                            Feature feat = feats.get(f);
                            try {
                                ModelItems subItems = feat.ListSubItems(ModelItemType.ITEM_DIMENSION);
                                if (subItems != null) {
                                    for (int d = 0; d < subItems.getarraysize(); d++) {
                                        ModelItem sub = subItems.get(d);
                                        if (sub instanceof BaseDimension && !dimsMap.containsKey(sub.GetId())) {
                                            dimsMap.put(sub.GetId(), (BaseDimension) sub);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[DrawingHandler] Features.ListSubItems: " + e.getMessage());
                }
            }

            System.out.println("[DrawingHandler] Found " + dimsMap.size() + " total dimensions in model " + model.GetFileName());

            // Project/show dimensions onto views
            for (BaseDimension dim : dimsMap.values()) {
                boolean shown = false;

                // Try showing on each orthographic view
                for (int v = 0; v < viewCount; v++) {
                    View2D view = views.get(v);
                    String vName = view.GetName();
                    // Prefer orthographic views (Front, Top, Right, etc.) over isometric
                    if (vName != null && vName.toLowerCase().contains("iso") && viewCount > 1) {
                        continue;
                    }

                    try {
                        DrawingDimensionShowInstructions inst = pfcView2D.DrawingDimensionShowInstructions_Create(view, (ComponentPath) null);
                        dim.Show(inst);
                        shown = true;
                        shownCount++;
                        System.out.println("[DrawingHandler] Successfully showed dim " + dim.GetId() + " (" + dim.GetName() + " = " + dim.GetDimValue() + ") on view '" + vName + "'");
                        break;
                    } catch (Exception e) {
                        // Dimension not orientable/projectable on this view
                    }
                }

                // If not projectable on specific orthographic views, try default Show(null)
                if (!shown) {
                    try {
                        dim.Show(null);
                        shown = true;
                        shownCount++;
                        System.out.println("[DrawingHandler] Showed dim " + dim.GetId() + " (" + dim.GetName() + ") via default Show(null)");
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            System.err.println("[DrawingHandler] Error showing dimensions: " + e.getMessage());
        }
        return shownCount;
    }

    private String prepareTemplate(String template, String workingDir) {
        File f = new File(template);
        File sourceFile = null;

        if (f.isAbsolute() && f.exists()) {
            sourceFile = f;
        } else {
            // Check working dir
            if (workingDir != null) {
                File workF = new File(workingDir, template);
                if (workF.exists()) sourceFile = workF;
                else {
                    File workFDrw = new File(workingDir, template + ".drw");
                    if (workFDrw.exists()) sourceFile = workFDrw;
                }
            }

            // Check Creo templates dir
            if (sourceFile == null) {
                String[] dirs = {
                    "C:\\Program Files\\PTC\\Creo 9.0.10.0\\Common Files\\templates",
                    "C:\\Program Files\\PTC\\Creo 10.0.0.0\\Common Files\\templates",
                    "C:\\Program Files\\PTC\\Creo 8.0.0.0\\Common Files\\templates"
                };
                for (String d : dirs) {
                    File tpl = new File(d, template);
                    if (tpl.exists()) { sourceFile = tpl; break; }
                    File tplDrw = new File(d, template + ".drw");
                    if (tplDrw.exists()) { sourceFile = tplDrw; break; }
                }
            }
        }

        // If found outside working dir, copy to working dir so Creo can find it by short name
        if (sourceFile != null && workingDir != null) {
            File dest = new File(workingDir, sourceFile.getName());
            if (!dest.exists()) {
                try {
                    java.nio.file.Files.copy(sourceFile.toPath(), dest.toPath());
                } catch (Exception e) {
                    System.err.println("[DrawingHandler] Could not copy template to working dir: " + e.getMessage());
                }
            }
            // Return base name without extension for ProName compatibility
            String name = sourceFile.getName();
            if (name.toLowerCase().endsWith(".drw")) {
                name = name.substring(0, name.length() - 4);
            }
            return name;
        }

        // Fallback: strip directory and extension to stay under 31 chars
        String name = f.getName();
        if (name.toLowerCase().endsWith(".drw")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
}

