# The Practical AI-Driven CAD Engineering Playbook
### Automating PTC Creo Parametric with Modern LLM Pair Programmers

Welcome to the **CAD AI Playbook**. While [`architecture.md`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/architecture.md) explains the deep internals and [`README.md`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/README.md) covers setup, this playbook is a **hands-on practitioner's manual**. It teaches engineers and developers how to talk to Creo using AI, run production automation workflows, extend the bridge with custom features, and troubleshoot native CAD exceptions.

---

## 1. CAD Prompt Engineering Guide

LLM agents connected to this MCP server have direct programmatic control over your CAD session. However, because CAD models are deterministic mathematical B-Reps with strict geometric rules, the way you prompt your AI makes a huge difference.

### 1.1 The Golden Rules of CAD Prompting
1. **Always Inspect First**: Instruct the AI to call `creo_get_active_model` or `creo_list_features` before executing modifications.
2. **Regenerate After Modifying**: Remind the agent that changing parameters or geometry requires a regeneration call (`creo_regenerate_model`) to rebuild the solid geometry.
3. **Specify Explicit Units/Types**: When setting parameters, specify the expected type (`DOUBLE`, `STRING`, `INTEGER`, or `BOOLEAN`) to ensure clean metadata.

---

### 1.2 Master Prompt Templates

#### Template A: The "One-Shot Drawing & PDF Release"
> *"Inspect the active 3D part on my screen. Verify its features and parameters. Then, generate a standard 2D engineering drawing using the C-size template (`c_drawing`), show all model dimensions on the views, and export a clean vector PDF to my Documents folder."*

#### Template B: The "Parametric Design Sweep"
> *"Read the current parameters on `bracket.prt`. I want to test a design variant: set `THICKNESS` to 12.5, set `HOLE_DIAMETER` to 8.0, and change `MATERIAL` to 'STEEL_AISI_1018'. Regenerate the model, confirm the geometry built without errors, and export a STEP file named `bracket_v2.stp`."*

#### Template C: The "Drawing Dimensions Refresh"
> *"The dimensions on my active drawing are out of sync or missing. Please call `creo_show_drawing_dimensions` on the active drawing to project all driving dimensions onto the orthographic views, regenerate the sheet, and repaint the window."*

#### Template D: The "Enterprise Metadata Audit"
> *"Audit all models currently open in Creo session. Check each model for required enterprise parameters: `DESCRIPTION`, `MODELED_BY`, `DRAWING_REVISION`, and `PTC_MATERIAL_NAME`. If any required parameter is missing, add it with appropriate default values, then save the models."*

---

## 2. Production CAD Automation Recipes

### Recipe 1: 1-Click Drawing Generation & PDF Export
This recipe automates the entire 2D drafting release cycle from a 3D part model:

```python
from creo_mcp.client import CreoBridgeClient

client = CreoBridgeClient("http://127.0.0.1:9090")

# 1. Query active solid model
status = client.get_active_model()
part_name = status["model"]["filename"] # e.g. "motor_bracket.prt"
base_name = part_name.split(".")[0]

# 2. Automatically generate drawing with views & dimensions
drw_res = client.create_drawing(
    model_name=part_name,
    drawing_name=f"{base_name}_drw",
    template="c_drawing",
    display=True,
    activate=True,
    show_dimensions=True
)
print("Drawing Created:", drw_res["drawingName"], "| Dimensions Shown:", drw_res["dimensionsShown"])

# 3. Export vector PDF for manufacturing
pdf_res = client.export_pdf(filename=f"{base_name}_drawing.pdf")
print("Vector PDF Saved:", pdf_res["absolutePath"])
```

---

### Recipe 2: Automated Quality & Parameter Compliance Check
Before releasing CAD files to PDM/PLM (Windchill, Teamcenter), verify mandatory corporate metadata:

```python
MANDATORY_PARAMS = {
    "PROJECT": ("STRING", "PROJECT_TITAN"),
    "DESIGNER": ("STRING", "AI_AUTOMATION"),
    "CONFIDENTIAL": ("BOOLEAN", "TRUE"),
    "TOLERANCE_CLASS": ("STRING", "ISO_2768_m")
}

def audit_and_apply_compliance(client: CreoBridgeClient, model_name: str = None):
    existing = client.list_parameters(model_name=model_name)
    present_keys = {p["name"].upper(): p["value"] for p in existing.get("parameters", [])}

    for param_name, (param_type, default_val) in MANDATORY_PARAMS.items():
        if param_name not in present_keys:
            client.set_parameter(name=param_name, value=default_val, param_type=param_type, model_name=model_name)
            print(f"[AUDIT] Added missing parameter {param_name} = {default_val}")
        else:
            print(f"[AUDIT] OK: {param_name} = {present_keys[param_name]}")

    client.save_model(model_name=model_name)
```

---

### Recipe 3: Batch Directory STEP Export
Convert an entire folder of proprietary Creo files (`.prt`, `.asm`) into neutral STEP files (`.stp`) for suppliers:

```python
import os
from creo_mcp.client import CreoBridgeClient

client = CreoBridgeClient("http://127.0.0.1:9090")
cad_folder = r"C:\Projects\Suspension_Assembly"

for fname in os.listdir(cad_folder):
    if fname.lower().endswith((".prt", ".asm")):
        fpath = os.path.join(cad_folder, fname)
        print(f"Processing: {fname}...")
        
        # Open in Creo
        open_res = client.open_model(file_path=fpath, display=False, activate=True)
        if open_res.get("success"):
            # Export STEP
            step_name = fname.rsplit(".", 1)[0] + ".stp"
            client.export_step(filename=step_name, model_name=fname)
            print(f" -> Exported {step_name}")
```

---

## 3. Developer Guide: Extending the Bridge

Adding a new CAD capability to this system takes under 10 minutes. Follow the standard three-layer pattern below.

### Example: Adding Mass Properties Query (`creo_get_mass_properties`)

#### Step 1: Add Java Handler in `java-bridge`
In `com.creomcp.bridge.handler.ModelHandler.java`:
```java
// Inside execute() method:
if ("mass_props".equalsIgnoreCase(action)) {
    Model model = mgr.resolveModel(modelName);
    if (!(model instanceof Solid)) {
        return JsonUtil.createError("Model is not a Solid part or assembly.");
    }
    Solid solid = (Solid) model;
    
    // Call J-Link Mass Property API
    com.ptc.pfc.pfcSolid.MassProperty props = solid.GetMassProperty(null);
    
    Map<String, Object> res = JsonUtil.createSuccess("Mass properties calculated.");
    res.put("mass", props.GetMass());
    res.put("volume", props.GetVolume());
    res.put("surfaceArea", props.GetSurfaceArea());
    res.put("density", props.GetDensity());
    return res;
}
```

In `MainBridgeServer.java`, register the endpoint:
```java
server.createContext("/api/model/mass_props", new ModelHandler("mass_props"));
```

#### Step 2: Add Python Client Method in `mcp-server`
In `creo_mcp/client.py`:
```java
def get_mass_properties(self, model_name: Optional[str] = None) -> Dict[str, Any]:
    params = {}
    if model_name:
        params["model"] = model_name
    return self._get("/api/model/mass_props", params=params)
```

#### Step 3: Expose MCP Tool in `mcp-server`
In `creo_mcp/server.py`:
```python
@mcp.tool()
def creo_get_mass_properties(model_name: str = "") -> str:
    """Retrieve mass properties (mass, volume, surface area, center of gravity) for a solid model.
    
    Args:
        model_name: Target model name. If omitted, uses active model.
    """
    res = client.get_mass_properties(model_name=model_name or None)
    return json.dumps(res, indent=2)
```

Rebuild via `build.bat` &mdash; the new tool is immediately accessible to your AI agents!

---

## 4. J-Link Error Code Decoder & Troubleshooting FAQ

When interacting with native CAD geometry, PTC Creo throws CIP exceptions. The Java Bridge intercepts and maps these into structured messages. Use this cheat-sheet to diagnose errors:

| CIP Exception Code | What It Means | Root Cause & Resolution |
| :--- | :--- | :--- |
| **`XToolkitCantModify`** | Resource locked / Mutex conflict. | Another external process (or another running instance of the bridge/Creoson) is attached to Creo's J-Link port. **Kill old Java processes (`taskkill /F /IM java.exe`) and restart the bridge.** |
| **`XStringTooLong`** | String exceeded buffer limit. | J-Link entity names map to C++ `ProName` (max 31 characters). Never pass full absolute paths to `CreateDrawingFromTemplate` or model names. Use the bridge's automatic staging mechanism. |
| **`XToolkitNotFound`** | Entity not found in session or disk. | The requested template, model, feature, or parameter does not exist. Verify spelling or check whether the file is in Creo's working directory. |
| **`XToolkitFound`** | Entity already exists in memory. | Occurs when trying to create a drawing or model with a name that is already loaded in the active session. Use a unique name or erase the old model from memory first. |
| **`XToolkitBadContext`** | Operation invalid for this model type. | For example, attempting to call `Solid.Regenerate()` on a 2D Drawing (`.drw`) rather than a 3D Part (`.prt`). Ensure the target model matches the expected type. |
| **`XToolkitDeadLock`** | Geometry regeneration cycle. | A circular parametric relation exists in the CAD model (e.g., parameter `A` drives dimension `d1`, which drives parameter `A`). Correct the relation in Creo's Tools &rarr; Relations dialog. |

---

## 5. Summary of Recommended Practices

* **Keep Creo Running in the Background**: Launch PTC Creo at the start of your workday; the Java Bridge latches onto the running instance in under 1 second without restarting CAD.
* **Keep the Bridge Daemon Alive**: Run `run_bridge.bat` once; it stays alive listening on port `9090` and handles reconnections automatically.
* **Use Automated Tests Before Big Tasks**: Running `python test_bridge.py` verifies all 7 subsystems (JNI, socket, feature tree, parameters, regeneration, drawings, and vector PDF) in ~10 seconds.

