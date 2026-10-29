# Creo MCP: AI-Powered CAD Automation for PTC Creo Parametric

[![Model Context Protocol](https://img.shields.io/badge/MCP-2.x-blue.svg)](https://modelcontextprotocol.io/)
[![PTC Creo](https://img.shields.io/badge/PTC_Creo-9.0_%7C_10.0_%7C_11.0-orange.svg)](https://www.ptc.com/en/products/creo)
[![Java](https://img.shields.io/badge/Java-17_LTS-red.svg)](https://adoptium.net/)
[![Python](https://img.shields.io/badge/Python-3.10+-yellow.svg)](https://www.python.org/)

A high-performance, decoupled **Model Context Protocol (MCP)** server and **standalone Java Bridge** connecting modern LLM agents (Claude Desktop, Antigravity, Cursor, ChatGPT) directly to **PTC Creo Parametric**.

---

## 1. The Story of Our Build

### The Genesis
Engineers spend countless hours performing repetitive CAD tasks: checking parameters, generating standard 2D views, verifying title blocks, exporting neutral files, and dimensioning drawings. While modern AI models excel at code and language, connecting an AI agent to an industrial, desktop-bound parametric CAD system like **PTC Creo Parametric** has historically been a massive barrier.

PTC Creo does not offer a native REST API or cloud endpoints for desktop automation. Its primary extension interfaces are C++ (Pro/TOOLKIT) and Java (J-Link), both tied strictly to native memory, proprietary DLLs, and out-of-process IPC sockets.

### Reverse-Engineering Creoson
Our journey began by reverse-engineering **Creoson 3.0.2** (`creoson-core`, `creoson-json`, and `creoson-server`). Creoson proved that PTC Creo could be automated out-of-process using J-Link, but dissecting its source code revealed significant architectural burdens:
* It was locked into a heavy, decade-old **Eclipse Equinox OSGi** plugin container.
* It carried legacy Java 8 dependencies, bloated Jetty servlet bundles, and required manual Maven compilation across dozens of nested sub-projects.
* It frequently locked up or crashed silently when encountering native CAD exceptions.

We made an intentional architectural pivot: **Build a modern, two-tier system from scratch with zero legacy bloat.**
1. **A lightweight, standalone Java 17 daemon (`java-bridge`)** using native JDK HTTP servers and bundled Jackson serialization to interface directly with Creo's asynchronous J-Link API (`pfcasync.jar`).
2. **A clean, official Python MCP server (`mcp-server`)** implementing the standard Model Context Protocol over `stdio`.

### The Five Engineering Hurdles & Breakthroughs

#### Hurdle 1: The MCP 2.x Architecture Evolution
Early MCP documentation referenced `FastMCP`. However, with the release of the official Python `mcp 2.x` SDK, `FastMCP` was restructured in favor of `mcp.server.mcpserver.MCPServer`. We updated the server architecture to natively support the latest asynchronous `stdio` event loops and Pydantic schema validation.

#### Hurdle 2: The JRE Class Version Conflict (69.0 vs 61.0)
During initial bridge compilation, VS Code's internal language server (running on an embedded JRE 25) automatically built `.class` files with bytecode version **69.0** into the `bin/` folder. When executed on the system's Java 17 runtime (bytecode version **61.0**), Java threw `java.lang.UnsupportedClassVersionError`.
* **The Fix**: We decoupled IDE outputs into `.bin-vscode/` and crafted [`build.bat`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/build.bat) using `javac --release 17 -d bin`, guaranteeing bytecode that runs reliably across any Java 17, 21, or 25 runtime.

#### Hurdle 3: Claude Desktop's Python Environment Sandbox
When Claude Desktop launched the MCP server, it spawned an unconfigured system `Python 3.7` instead of the machine's primary `Python 3.14`, resulting in `ModuleNotFoundError: No module named 'httpx'`.
* **The Fix**: We pinned the explicit Python binary path in Claude Desktop's configuration and introduced [`setup.bat`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/setup.bat) to automatically create and isolate virtual environments (`.venv`).

#### Hurdle 4: The 31-Character J-Link Buffer Limit (`XStringTooLong`)
When automating 2D drawing creation, passing absolute template paths like:
`C:\Program Files\PTC\Creo 9.0.10.0\Common Files\templates\c_drawing.drw` (78 characters)
triggered an instant CIP native crash: `com.ptc.cipjava.jxthrowable: XStringTooLong`.
* **The Breakthrough**: In PTC's underlying C++ kernel, entity names are fixed-length `ProName` buffers (maximum 31 characters). We developed **Dynamic Workspace Staging** in [`DrawingHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/DrawingHandler.java): the bridge automatically stages templates into Creo's active working directory and supplies short names (e.g. `c_drawing`), eliminating string overflows while supporting arbitrary templates.

#### Hurdle 5: The Missing Dimensions on 2D Drawings
Upon generating drawings from templates, the 2D orthographic views appeared as blank wireframes without dimensions. In Creo, 3D driving dimensions are authored on solid features and remain hidden on drawings until explicitly projected.
* **The Breakthrough**: By reverse-engineering `com.ptc.pfc.pfcView2D.pfcView2D`, we discovered `DrawingDimensionShowInstructions_Create(view, null)`. We built a projection engine that loops through model features, attaches dimensions to their corresponding orthogonal projection views, and triggers an instantaneous OpenGL viewport redraw (`window.Refresh()`, `window.Repaint()`).

---

## 2. Requirements & Prerequisites

Before deploying the bridge, ensure the target PC meets the following:

| Component | Minimum Requirement | Recommended | Notes |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10 (64-bit) | Windows 11 (64-bit) | Creo J-Link requires Windows 64-bit IPC message queues. |
| **PTC Creo Parametric** | Creo 4.0 (64-bit) | Creo 9.0, 10.0, or 11.0 | Must be installed with the J-Link option (standard in PTC installs). |
| **Java Development Kit** | JDK 11 (64-bit) | JDK 17 LTS (64-bit) | **64-bit is mandatory** to load 64-bit DLL `pfcasyncmt.dll`. |
| **Python** | Python 3.10 (64-bit) | Python 3.11 / 3.12 / 3.14 | Required for `mcp 2.x` SDK and `httpx`. |

---

## 3. How to Make It Work on Any PC (Step-by-Step)

Follow these 4 simple steps to deploy and run this project on any computer:

### Step 1: Run One-Click Setup
Open Command Prompt (`cmd.exe`) in the project directory:
```cmd
cd C:\Users\<YOUR_USER>\CREO_MCP\creo_mcp
setup.bat
```
* This compiles the Java Bridge with `javac --release 17`.
* It creates a dedicated Python virtual environment (`mcp-server\.venv`).
* It installs all dependencies (`mcp`, `httpx`, `pydantic`).

### Step 2: Start PTC Creo Parametric
Launch **PTC Creo Parametric** on your desktop. Open any part (e.g. `prt0001.prt`) or start a new model.

### Step 3: Start the Java Bridge Daemon
In your terminal:
```cmd
cd java-bridge
run_bridge.bat
```
You will see:
```
==========================================================
   Creo MCP Java Bridge Server started successfully!
   Listening on: http://127.0.0.1:9090
==========================================================
[MainBridgeServer] Initializing connection to Creo...
[CreoSessionManager] J-Link native library 'pfcasyncmt' loaded successfully.
[CreoSessionManager] Successfully connected to Creo session! Working dir: C:\Users\...
```

### Step 4: Configure Your AI Client

#### For Claude Desktop
Open `%APPDATA%\Claude\claude_desktop_config.json` and add:
```json
{
  "mcpServers": {
    "creo": {
      "command": "cmd.exe",
      "args": ["/c", "C:\\Users\\<YOUR_USER>\\CREO_MCP\\creo_mcp\\mcp-server\\run_mcp.bat"]
    }
  }
}
```

#### For Google Antigravity
Open `~/.gemini/config/mcp_config.json` and add:
```json
{
  "mcpServers": {
    "creo": {
      "command": "python",
      "args": ["-m", "creo_mcp.server"],
      "env": {
        "PYTHONPATH": "C:\\Users\\<YOUR_USER>\\CREO_MCP\\creo_mcp\\mcp-server"
      }
    }
  }
}
```

#### Verification Test
Run the automated 7-stage test harness in a separate terminal:
```cmd
cd mcp-server
python test_bridge.py
```
If you see `[SUCCESS] All Creo MCP Bridge tests passed!`, your system is 100% operational!

---

## 4. Available MCP Tools (The 17 CAD Commands)

The server exposes **17 specialized CAD tools** categorized below:

### 4.1 Session & Connection Management
| Tool | Description | Example Prompt |
| :--- | :--- | :--- |
| `creo_get_status` | Returns bridge connectivity, working directory, and active CAD model. | *"What model is currently open on my Creo screen?"* |
| `creo_connect` | Handshakes and attaches to the active Creo process via J-Link IPC. | *"Connect to Creo with a 15-second timeout."* |
| `creo_disconnect` | Gracefully detaches the bridge from the active Creo process. | *"Disconnect from the active CAD session."* |

### 4.2 Model Management & Inspection
| Tool | Description | Example Prompt |
| :--- | :--- | :--- |
| `creo_get_active_model` | Retrieves filename, model type, common name, and modified flag. | *"Is the current part modified since last save?"* |
| `creo_list_models` | Lists all CAD parts, assemblies, and drawings in Creo's memory. | *"List all models currently in session."* |
| `creo_open_model` | Opens and displays a `.prt`, `.asm`, or `.drw` in a viewport. | *"Open C:\CAD\flange.prt and bring it to the front."* |
| `creo_save_model` | Saves the specified or active CAD model to disk. | *"Save the active part."* |
| `creo_regenerate_model` | Triggers a full geometric rebuild (`Solid.Regenerate()`). | *"Regenerate the solid geometry to update dimensions."* |

### 4.3 Parameter Engineering
| Tool | Description | Example Prompt |
| :--- | :--- | :--- |
| `creo_list_parameters` | Lists all metadata parameters (name, type, value, designation). | *"Show me all parameters on this model."* |
| `creo_get_parameter` | Reads the exact value and type of a specific parameter. | *"What is the MATERIAL parameter set to?"* |
| `creo_set_parameter` | Creates or updates parameters (`STRING`, `DOUBLE`, `INTEGER`, `BOOLEAN`). | *"Set TOTAL_LENGTH to 250.0 and MATERIAL to 'AL6061'."* |
| `creo_delete_parameter` | Deletes a parameter from the model. | *"Delete obsolete parameter TEMP_FLAG."* |

### 4.4 Feature Tree
| Tool | Description | Example Prompt |
| :--- | :--- | :--- |
| `creo_list_features` | Lists all features in the model (ID, name, type name, status). | *"List all cuts and protrusions in the active part."* |

### 4.5 2D Drawing Automation & Annotation
| Tool | Description | Example Prompt |
| :--- | :--- | :--- |
| `creo_create_drawing` | Generates a 2D drawing (.drw) from a 3D model with views, title block, and automatic dimension placement. | *"Generate a 2D drawing for this part using the C-size template and show all dimensions."* |
| `creo_show_drawing_dimensions` | Discovers and projects 3D driving dimensions onto drawing views. | *"Show all model dimensions on the active drawing views."* |

### 4.6 File Export Pipelines
| Tool | Description | Example Prompt |
| :--- | :--- | :--- |
| `creo_export_step` | Exports 3D models to STEP format (`.stp`). | *"Export this model to STEP format as motor_mount.stp."* |
| `creo_export_pdf` | Exports active drawing or 3D model to vector PDF. | *"Export the active drawing to a high-resolution PDF."* |

---

## 5. Version Dependencies & Compatibility Matrix

```
                      ┌──────────────────────────────────────┐
                      │          PTC Creo Parametric         │
                      │  Creo 4.0 | 7.0 | 8.0 | 9.0 | 10 | 11 │
                      └──────────────────┬───────────────────┘
                                         │
        ┌────────────────────────────────┴────────────────────────────────┐
        │                                                                 │
        ▼                                                                 ▼
┌──────────────────┐                                            ┌──────────────────┐
│   Java Runtime   │                                            │  Python Runtime  │
│  64-bit OpenJDK  │                                            │  64-bit CPython  │
├──────────────────┤                                            ├──────────────────┤
│ Java 17 LTS (Rec)│  Compiled with --release 17                │ Python 3.10+     │
│ Java 21 LTS      │  Runs bytecode out-of-the-box              │ Python 3.11      │
│ Java 25 (Latest) │  Fully compatible                          │ Python 3.12      │
│ Java 11 (Legacy) │  Supported (set --release 11 in build.bat) │ Python 3.14 (Rec)│
└──────────────────┘                                            └──────────────────┘
```

### Auto-Detection Logic
In [`run_bridge.bat`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/run_bridge.bat), the script automatically inspects `C:\Program Files\PTC\` for installed Creo versions. If your Creo installation resides on a non-standard drive or folder (e.g. `D:\PTC\Creo`), specify the variable before launching:
```cmd
set "CREO_COMMON=D:\PTC\Creo 9.0.10.0\Common Files"
run_bridge.bat
```

---

## 6. Project Layout

```
creo_mcp/
├── README.md                      # System Story, Setup & Tool Catalog
├── architecture.md               # Technical Deep-Dive & Transaction Spec
├── CAD_AI_PLAYBOOK.md             # Practical Recipes & Engineering Playbook
├── setup.bat                      # One-click automated setup
│
├── java-bridge/                   # Java 17 Standalone Daemon
│   ├── src/com/creomcp/bridge/
│   │   ├── MainBridgeServer.java  # HTTP Server entry point (port 9090)
│   │   ├── CreoSessionManager.java# J-Link IPC lifecycle & error mapper
│   │   ├── handler/               # Domain REST handlers (Drawing, Model, etc.)
│   │   └── util/JsonUtil.java     # Jackson serialization wrapper
│   ├── lib/                       # pfcasync.jar & jackson-*.jar
│   ├── bin/                       # Compiled Java 17 bytecode
│   ├── build.bat                  # Clean compile script
│   └── run_bridge.bat             # Auto-detecting daemon runner
│
└── mcp-server/                    # Python MCP Server (mcp 2.x)
    ├── creo_mcp/
    │   ├── __init__.py
    │   ├── client.py              # HTTP client to Java Bridge
    │   └── server.py              # 17 Registered MCP CAD Tools
    ├── test_bridge.py             # 7-stage automated regression suite
    ├── requirements.txt           # mcp, httpx, pydantic
    └── run_mcp.bat                # MCP runner with venv auto-detection
```

---

## 7. License

Licensed under the Apache License 2.0. Built with pride for CAD engineers, mechanical designers, and AI developers.
