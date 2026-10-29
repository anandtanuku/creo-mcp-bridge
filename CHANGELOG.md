# Changelog

All notable changes to the Creo MCP Bridge are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [2.0.0] - 2026-10-29

### Added
- **Drawing automation** (`/api/drawing/create`) — full 2D engineering drawing generation from a 3D model using a Creo template (`CreateDrawingFromTemplate`)
- **Dimension projection engine** (`/api/drawing/show_dimensions`) — traverses feature tree via `ListSubItems(ModelItemType.ITEM_DIMENSION)` and projects dimensions onto orthographic views using `DrawingDimensionShowInstructions_Create()`
- **PDF export** (`/api/export/pdf`) — vector PDF generation from Drawing and Solid models using `PDFExportInstructions_Create()`
- `creo_create_drawing` MCP tool with `show_dimensions` flag for one-call drawing automation
- `creo_show_drawing_dimensions` MCP tool for standalone dimension projection
- `creo_export_pdf` MCP tool

### Fixed
- **`XStringTooLong`** — template and drawing names exceeding Creo's internal 31-character `ProName` C++ buffer; fixed by staging template to working directory and stripping path + extension before passing to `CreateDrawingFromTemplate()`
- **`XToolkitFound`** — duplicate drawing name collision; fixed by appending random suffix to generated drawing names
- **Blank drawing views** — dimensions not appearing on drawing after creation; root cause was calling `dim.Show(null)` without a view target; fixed by switching to `pfcView2D.DrawingDimensionShowInstructions_Create(view, null)` with an explicit `View2D` target per dimension
- Isometric views deprioritized in dimension placement loop — orthographic views (Front, Top, Right) preferred for readability

---

## [1.0.0] - 2026-10-29

### Added

**Java Bridge — Core Infrastructure**
- `MainBridgeServer` — embedded `com.sun.net.httpserver.HttpServer` listening on `127.0.0.1:9090` with a 4-thread fixed pool (`Executors.newFixedThreadPool(4)`)
- `CreoSessionManager` — Singleton J-Link session manager with `synchronized` methods, auto-reconnect logic, and liveness check via `AsyncConnection.IsRunning()`
- `BaseHandler` — abstract Template Method base class providing CORS headers, OPTIONS preflight handling, request body reading, query parameter parsing, and uniform JSON response writing
- `JsonUtil` — Jackson `ObjectMapper` wrapper with `TypeReference`-based generic deserialization, success/error response builders, and defensive string escaping
- `/api/shutdown` — graceful server shutdown with 500ms delay to ensure response delivery

**Connection Endpoints**
- `GET /api/status` — session health check, active model info, current working directory
- `POST /api/connect` — J-Link connection to running Creo instance via `AsyncConnection_Connect(null, null, null, timeout)`
- `POST /api/disconnect` — graceful disconnect with 2-second drain timeout

**Model Endpoints**
- `GET /api/model/active` — retrieve active model filename, type, common name, modified flag
- `GET /api/model/list` — list all models loaded in session
- `POST /api/model/open` — open model by filename or absolute path, with `display` and `activate` options
- `POST /api/model/save` — save model to disk
- `POST /api/model/regenerate` — regenerate solid model geometry (`Solid.Regenerate(null)`)

**Parameter Endpoints**
- `GET /api/parameter/list` — list all model parameters with optional glob pattern filter
- `GET /api/parameter/get` — get a parameter's value, type, designation, and description
- `POST /api/parameter/set` — set or create a parameter (STRING / DOUBLE / INTEGER / BOOLEAN) with automatic type inference
- `POST /api/parameter/delete` — delete a parameter from a model

**Feature Endpoints**
- `GET /api/feature/list` — list all features in a solid model with optional type filter (id, name, typeName, status, visibility)

**Export Endpoints**
- `POST /api/export/step` — export 3D model to STEP format (ISO 10303)

**Python MCP Server — v1.0.0 Tools (13 tools)**
- `creo_get_status`
- `creo_connect`
- `creo_disconnect`
- `creo_get_active_model`
- `creo_list_models`
- `creo_open_model`
- `creo_save_model`
- `creo_regenerate_model`
- `creo_list_parameters`
- `creo_get_parameter`
- `creo_set_parameter`
- `creo_delete_parameter`
- `creo_list_features`
- `creo_export_step`

### Fixed during v1.0.0 development
- **`UnsatisfiedLinkError`** — `pfcasyncmt.dll` not found at startup; fixed by prepending Creo `Common Files\lib\obj\` to `PATH` in `run_bridge.bat` before JVM launch
- **`UnsupportedClassVersionError` (class file version 69.0 vs 61.0)** — VS Code compiled with Java 25 bytecode, incompatible with Java 17 runtime; fixed by compiling with `javac --release 17 -d bin` explicitly, bypassing VS Code's `.bin-vscode` output
- **`ModuleNotFoundError: No module named 'httpx'`** in Claude Desktop — system `python` resolving to wrong interpreter; fixed by specifying full Python 3.14 binary path in `claude_desktop_config.json`
- **`FastMCP` import failure** — MCP SDK 2.3.0 removed legacy `FastMCP`; fixed by migrating to `from mcp.server.mcpserver import MCPServer`

---

---

## [3.0.0] - Planned — AI-Driven Geometry Modification

> Core theme: **from read-only to read-write** — AI that can directly modify model geometry.

### Expected — Dimension Control
- `creo_get_dimension` — read current dimension value by name (`d1`, `d2`, or user-defined name) via `BaseDimension.GetDimValue()`
- `creo_set_dimension` — **change a dimension value** (`BaseDimension.SetDimValue()`) and trigger regeneration — the core of AI-driven parametric design
- `creo_list_dimensions` — list all dimensions in a model with current values, names, and owning feature

### Expected — Feature Suppression
- `creo_suppress_feature` — suppress a feature by name or ID (`Feature.Suppress()`) — hide it from regen without deleting
- `creo_resume_feature` — un-suppress a suppressed feature (`Feature.Resume()`)

### Expected — Mass Properties & Analysis
- `creo_get_mass_properties` — retrieve volume, surface area, mass, center of gravity, and moments of inertia (`Solid.GetMassProperty()`)
- `creo_measure_distance` — measure distance between two geometry references using `pfcAnalysis` namespace

---

## [3.1.0] - Planned — Export Formats & Visual Feedback

> Core theme: **broader output formats** for manufacturing and visual review.

### Expected — Export
- `creo_export_stl` — export 3D solid to STL format for 3D printing pipelines
- `creo_export_dxf` — export 2D drawing to DXF/DWG for CNC and laser cutting workflows
- `creo_export_iges` — export to IGES format (secondary universal CAD exchange standard)
- `creo_capture_screenshot` — save PNG/JPEG of the current Creo graphics window (`Window.SaveBitmap()`) — enables visual confirmation of AI actions

### Expected — View Control
- `creo_set_view_orientation` — programmatically set standard view (FRONT, TOP, RIGHT, ISOMETRIC) via `Window.SetStandardOrientation()`
- `creo_set_display_style` — switch between Shaded, Wireframe, Hidden Line display modes
- `creo_zoom_fit` — fit model to window (`Window.ZoomFit()`)

---

## [4.0.0] - Planned — Assembly Intelligence

> Core theme: **assembly-level awareness** — understand multi-part structures.

### Expected — Assembly Operations
- `creo_list_components` — list all parts and sub-assemblies in an assembly with component paths (`Assembly.ListChildren()`)
- `creo_get_component_transform` — retrieve position and orientation matrix of each component in an assembly (`ComponentPath.GetTransform()`)
- `creo_check_interference` — detect clashing components in an assembly (`Assembly.CheckInterfere()`) — AI-assisted design validation

### Expected — Layer Control
- `creo_list_layers` — list all layers and their visibility status (`Model.ListLayers()`)
- `creo_set_layer_visibility` — show or hide layers (datum axes, planes, coordinate systems) via `Layer.SetStatus()`

### Expected — Family Tables
- `creo_list_family_instances` — list all family table variants (rows) of a part (`FamilyTable.ListRows()`)
- `creo_activate_instance` — switch to a specific family table instance (e.g. switch between M6, M8, M10 bolt variants)

---

## [4.1.0] - Planned — Equation-Driven Design

> Core theme: **relations and equations** — AI that understands and writes parametric logic.

### Expected — Relations
- `creo_list_relations` — list all equations currently driving a model (`Solid.ListRelations()`)
- `creo_add_relation` — add a new equation (e.g. `d2 = d1 * 2.0`, `WEIGHT = VOLUME * DENSITY`) via `Solid.AddRelation()`
- `creo_delete_relation` — remove an existing relation

### Expected — Drawing Enhancements
- `creo_add_drawing_note` — add a text annotation/note to a drawing at a specified location (`Drawing.CreateNote()`)
- `creo_list_drawing_views` — list all views on the current drawing sheet with names and orientations
- `creo_add_drawing_view` — programmatically add a new view (general, projection, detail) to an existing drawing
- `creo_set_sheet_size` — change drawing sheet format (A4, A3, A, B, C, D)

---

## [5.0.0] - Planned — Full Session Control

> Core theme: **complete automation** — AI controls every aspect of the Creo session.

### Expected — Config & Session
- `creo_get_config` — read a `config.pro` option value at runtime (`Session.GetConfigOption()`)
- `creo_set_config` — set a config option for the current session (`Session.SetConfigOption()`)
- `creo_change_directory` — change Creo's working directory (`Session.ChangeDirectory()`)
- `creo_run_mapkey` — execute an arbitrary Creo mapkey or trail file macro (`Session.RunMacro()`) — enables any UI action to be scripted

### Expected — Infrastructure
- SSE (Server-Sent Events) MCP transport for cloud/web deployment (replace stdio)
- Support for multiple simultaneous Creo sessions
- WebSocket-based real-time event streaming (model change notifications)
- REST API authentication token for multi-user environments

