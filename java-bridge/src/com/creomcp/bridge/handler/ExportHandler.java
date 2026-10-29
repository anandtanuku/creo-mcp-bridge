package com.creomcp.bridge.handler;

import com.creomcp.bridge.CreoSessionManager;
import com.creomcp.bridge.util.JsonUtil;
import com.ptc.cipjava.jxthrowable;
import com.ptc.pfc.pfcArgument.pfcArgument;
import com.ptc.pfc.pfcExport.PDFExportInstructions;
import com.ptc.pfc.pfcExport.PDFOption;
import com.ptc.pfc.pfcExport.PDFOptionType;
import com.ptc.pfc.pfcExport.PDFOptions;
import com.ptc.pfc.pfcExport.pfcExport;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcModel.STEPExportInstructions;
import com.ptc.pfc.pfcModel.pfcModel;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles /api/export/step and /api/export/pdf.
 */
public class ExportHandler extends BaseHandler {

    private final String format;

    public ExportHandler(String format) {
        this.format = format;
    }

    @Override
    protected Map<String, Object> execute(HttpExchange exchange) throws Exception {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            return JsonUtil.createError("POST required for export");
        }

        CreoSessionManager mgr = CreoSessionManager.getInstance();
        Map<String, Object> body = readJsonBody(exchange);

        String filename = (String) body.get("filename");
        String modelName = (String) body.get("model");

        Model model = mgr.resolveModel(modelName);

        if ("step".equalsIgnoreCase(format)) {
            if (filename == null || filename.trim().isEmpty()) {
                String base = model.GetFileName();
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                filename = base + ".stp";
            }
            if (!filename.toLowerCase().endsWith(".stp") && !filename.toLowerCase().endsWith(".step")) {
                filename = filename + ".stp";
            }

            try {
                STEPExportInstructions instructions = pfcModel.STEPExportInstructions_Create(null);
                model.Export(filename, instructions);

                File f = resolveOutputFile(filename, mgr.getCurrentDirectory());
                Map<String, Object> res = JsonUtil.createSuccess("Model exported to STEP successfully: " + filename);
                res.put("model", model.GetFileName());
                res.put("outputFile", filename);
                res.put("absolutePath", f.getAbsolutePath());
                return res;

            } catch (jxthrowable jxe) {
                return JsonUtil.createError("STEP export error: " + CreoSessionManager.formatCipException(jxe));
            }

        } else if ("pdf".equalsIgnoreCase(format)) {
            if (filename == null || filename.trim().isEmpty()) {
                String base = model.GetFileName();
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                filename = base + ".pdf";
            }
            if (!filename.toLowerCase().endsWith(".pdf")) {
                filename = filename + ".pdf";
            }

            try {
                PDFExportInstructions pxi = pfcExport.PDFExportInstructions_Create();
                PDFOptions opts = PDFOptions.create();

                // Do not auto-launch external viewer
                PDFOption optViewer = pfcExport.PDFOption_Create();
                optViewer.SetOptionType(PDFOptionType.PDFOPT_LAUNCH_VIEWER);
                optViewer.SetOptionValue(pfcArgument.CreateBoolArgValue(false));
                opts.append(optViewer);

                pxi.SetOptions(opts);

                System.out.println("[ExportHandler] Exporting model/drawing '" + model.GetFileName() + "' to PDF: " + filename);
                model.Export(filename, pxi);

                File f = resolveOutputFile(filename, mgr.getCurrentDirectory());
                Map<String, Object> res = JsonUtil.createSuccess("Exported to PDF successfully: " + filename);
                res.put("model", model.GetFileName());
                res.put("outputFile", filename);
                res.put("absolutePath", f.getAbsolutePath());
                return res;

            } catch (jxthrowable jxe) {
                return JsonUtil.createError("PDF export error: " + CreoSessionManager.formatCipException(jxe));
            }

        } else {
            return JsonUtil.createError("Unsupported export format: " + format);
        }
    }

    private File resolveOutputFile(String filename, String workingDir) {
        File f = new File(filename);
        if (f.isAbsolute()) {
            return f;
        }
        if (workingDir != null) {
            return new File(workingDir, filename);
        }
        return f;
    }
}
