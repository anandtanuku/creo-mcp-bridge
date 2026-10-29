Built an AI-to-CAD bridge integrating Claude/GPT with PTC Creo Parametric via a custom Java HTTP server (J-Link/JNI) and MCP-compliant Python tool layer — enabling natural-language automation of 3D model inspection, parameter editing, drawing generation, and STEP/PDF export

# Creo MCP Bridge — Deep Build Reference

> A thorough walk-through of every technology choice, every design pattern, and every meaningful line of code in this project.  
> **Stack**: Java 17 · J-Link · Jackson · com.sun.net.httpserver → HTTP → Python · httpx · MCP SDK 2.x

---

## Concepts Covered in This Project

Everything built here touches a real concept used in production software engineering. The table below maps each concept to where it appears in this codebase, so you can connect theory to practice.

### 🟦 Java & JVM Concepts

| Concept | Where It Appears | What to Look For |
|---|---|---|
| **Singleton Design Pattern** | `CreoSessionManager.java` | `private static final` eager instance, private constructor |
| **Synchronized / Monitor Locks** | `CreoSessionManager.java` | `synchronized` on every public method |
| **Abstract Classes & Template Method Pattern** | `BaseHandler.java` | `handle()` calls abstract `execute()` |
| **JNI (Java Native Interface)** | `CreoSessionManager.java` | `System.loadLibrary("pfcasyncmt")` |
| **Static Initializer Block** | `JsonUtil.java` | `static { mapper.enable(...) }` |
| **Thread Pools** | `MainBridgeServer.java` | `Executors.newFixedThreadPool(4)` |
| **Embedded HTTP Server** | `MainBridgeServer.java` | `com.sun.net.httpserver.HttpServer` |
| **Generic Type Erasure + TypeReference** | `JsonUtil.java` | `new TypeReference<Map<String,Object>>(){}` |
| **instanceof + Polymorphism** | `ModelHandler.java`, `ExportHandler.java` | `model instanceof Solid`, polymorphic `Export()` |
| **NIO File Operations** | `DrawingHandler.java` | `java.nio.file.Files.copy()` |
| **Strategy / Action Dispatch Pattern** | All Handlers | Constructor takes `action` string, if-else dispatch |
| **Fail-Fast Error Handling** | All Handlers | Exceptions bubble to `BaseHandler`, returned as JSON |
| **Defensive Nulling** | `CreoSessionManager.isConnected()` | Null out fields on unexpected exception |

### 🟨 Python Concepts

| Concept | Where It Appears | What to Look For |
|---|---|---|
| **Context Managers (`with`)** | `client.py` | `with httpx.Client() as client:` |
| **Type Hints & `Optional[X]`** | `client.py`, `server.py` | Function signatures with `->` and `Optional` |
| **Decorators** | `server.py` | `@mcp.tool()` on every tool function |
| **f-strings** | `client.py` | `f"{self.base_url}{path}"` |
| **Conditional dict building** | `client.py` | `if model_name: data["model"] = model_name` |
| **Module as entry point** | `server.py` | `if __name__ == "__main__": main()` |

### 🟩 System Design & Architecture Concepts

| Concept | Where It Appears |
|---|---|
| **Two-tier architecture** (Java + Python) driven by SDK constraints | Overall project structure |
| **Loopback-only binding** for security | `MainBridgeServer` — `127.0.0.1` not `0.0.0.0` |
| **HTTP as an IPC mechanism** | Java ↔ Python communication |
| **CORS & HTTP Preflight** | `BaseHandler` CORS headers |
| **Protocol Adapter Pattern** | Python MCP server adapts HTTP API to AI tool schema |
| **Factory Method Pattern** | `DrawingDimensionShowInstructions_Create()`, `ModelDescriptor_CreateFromFileName()` |
| **Tagged Union / Discriminant** | `ParamValue.Getdiscr()` in `ParameterHandler` |
| **Retry with Alternate Format** | `DrawingHandler` template name retry (with/without `.drw`) |

### 🟥 AI / MCP / Protocol Concepts

| Concept | Where It Appears |
|---|---|
| **Model Context Protocol (MCP)** | `server.py` — `MCPServer`, `@mcp.tool()` |
| **JSON-RPC 2.0 over stdio** | MCP server transport — `mcp.run()` |
| **Tool Schema Auto-generation** | `@mcp.tool()` reads type hints + docstrings |
| **AI Agent Tool Design** | Naming (`creo_*`), docstrings as tool descriptions |

---

## Follow-Up Concepts — What to Learn Next

These are the natural next steps if you want to go deeper in each layer of this stack.

### From this project → Next level

#### Java / Backend Engineering
| What You Did Here | Natural Next Step | Why It Matters |
|---|---|---|
| `com.sun.net.httpserver` | **Spring Boot** or **Quarkus** | Production REST APIs with DI, validation, security |
| `newFixedThreadPool(4)` | **CompletableFuture / Project Loom (Virtual Threads)** | Async non-blocking Java — Java 21's killer feature |
| Manual JSON with Jackson | **Spring MVC with `@RequestBody`** | Jackson auto-wired with annotation-driven binding |
| `synchronized` | **`ReentrantLock`, `java.util.concurrent`** | Fine-grained locking, `ReadWriteLock`, `Semaphore` |
| Basic OOP patterns | **SOLID principles, DI containers** | `@Inject`, `@Singleton` in frameworks |
| Manual HTTP routing | **JAX-RS / Jakarta EE** | Annotation-based routing (`@GET`, `@Path`) |

#### Python / AI Engineering
| What You Did Here | Natural Next Step | Why It Matters |
|---|---|---|
| Synchronous `httpx.Client` | **`httpx.AsyncClient` + `asyncio`** | Non-blocking I/O for high-throughput tool servers |
| `@mcp.tool()` with basic types | **Pydantic models as tool parameters** | Structured input validation, complex nested schemas |
| Single-tool MCP server | **Multi-server MCP orchestration** | Connect multiple tool servers to one AI agent |
| `json.dumps()` as return | **Structured content blocks** (MCP 2.x feature) | Return images, files, and typed data, not just strings |
| Manual retry logic | **Tenacity / `backoff` library** | Production-grade retry with exponential backoff |

#### AI / Agent Engineering
| What You Did Here | Natural Next Step | Why It Matters |
|---|---|---|
| Tool-calling via MCP | **LangChain / LlamaIndex agents** | Orchestration frameworks for multi-step AI reasoning |
| Claude via MCP | **OpenAI Function Calling / Gemini Tool Use** | Same concept, different protocol — know all three |
| Single AI ↔ Tool interaction | **Multi-agent systems (CrewAI, AutoGen)** | Multiple AI agents collaborating on a task |
| stdio transport | **SSE (Server-Sent Events) MCP transport** | Web-based MCP for cloud deployment |
| Prompt engineering in docstrings | **System prompt design, RAG** | Better tool descriptions = better AI decisions |

#### System Design
| What You Did Here | Natural Next Step | Why It Matters |
|---|---|---|
| Loopback HTTP bridge | **gRPC** | Typed, high-performance RPC — standard for microservices |
| Manual process management (`run_bridge.bat`) | **Docker + Docker Compose** | Containerized deployment, port mapping, healthchecks |
| Single-machine architecture | **Message queues (RabbitMQ, Kafka)** | Decouple components, handle async workloads at scale |
| Basic error responses | **OpenTelemetry tracing** | Distributed tracing across Java ↔ Python boundary |
| `run_bridge.bat` startup | **systemd / Windows Service** | Run as a background service that survives reboots |

---

## Table of Contents

1. [Why This Architecture?](#1-why-this-architecture)
2. [The Java Side](#2-the-java-side)
   - 2.1 [JsonUtil.java — The Jackson Wrapper](#21-jsonutiljava--the-jackson-wrapper)
   - 2.2 [CreoSessionManager.java — The J-Link Lifeline](#22-creosessionmanagerjava--the-j-link-lifeline)
   - 2.3 [MainBridgeServer.java — The HTTP Entry Point](#23-mainbridgeserverjava--the-http-entry-point)
   - 2.4 [BaseHandler.java — The Template Method](#24-basehandlerjava--the-template-method)
   - 2.5 [ConnectionHandler.java — Session Control](#25-connectionhandlerjava--session-control)
   - 2.6 [ModelHandler.java — CAD Model Operations](#26-modelhandlerjava--cad-model-operations)
   - 2.7 [ParameterHandler.java — Data Attached to Geometry](#27-parameterhandlerjava--data-attached-to-geometry)
   - 2.8 [FeatureHandler.java — The Feature Tree](#28-featurehandlerjava--the-feature-tree)
   - 2.9 [DrawingHandler.java — 2D Drawing Automation](#29-drawinghandlerjava--2d-drawing-automation)
   - 2.10 [ExportHandler.java — STEP and PDF Export](#210-exporthandlerjava--step-and-pdf-export)
3. [The Python Side](#3-the-python-side)
   - 3.1 [client.py — The HTTP Bridge Client](#31-clientpy--the-http-bridge-client)
   - 3.2 [server.py — The MCP Tool Layer](#32-serverpy--the-mcp-tool-layer)
4. [The Build System](#4-the-build-system)
5. [The IPC / OS Layer](#5-the-ipc--os-layer)
6. [Error Taxonomy — What Each Exception Means](#6-error-taxonomy--what-each-exception-means)
7. [Keyword Glossary](#7-keyword-glossary)

---

## 1. Why This Architecture?

### The fundamental constraint: J-Link requires a JVM

PTC Creo exposes its automation API exclusively through **J-Link**, a proprietary SDK distributed as `pfcasync.jar` plus a native DLL `pfcasyncmt.dll`. There is no REST API, no Python binding, and no COM/OLE interface. If you want to automate Creo programmatically, you **must** write Java (or another JVM language). This is the single biggest architectural driver.

### Why not write the whole thing in Java then?

The **Model Context Protocol (MCP)** SDK — the standard by which AI assistants like Claude call external tools — has official, well-maintained implementations only in **Python** and **TypeScript**. The Java ecosystem has no official MCP server library. Building one from scratch would mean re-implementing the MCP JSON-RPC 2.0 protocol, tool schema generation, and stdio transport by hand. That is months of unrelated work. So:

- **Java** owns the Creo layer (the only language that can talk to J-Link).  
- **Python** owns the MCP layer (the only practical language with an official MCP server SDK).  
- **HTTP on localhost** connects them — a trivial, debuggable, language-agnostic wire protocol.

### Why HTTP and not sockets, pipes, or RPC?

Plain named pipes or raw TCP sockets would work but are painful to debug (no browser, no `curl`). An HTTP interface means any developer can test any endpoint with a single `curl` command or a browser. The overhead of HTTP framing on loopback (`127.0.0.1`) is microseconds — entirely irrelevant for a CAD automation system where operations take hundreds of milliseconds.

### Why `com.sun.net.httpserver` over Tomcat / Jetty / Spring?

`com.sun.net.httpserver.HttpServer` ships inside the JDK itself (in the `jdk.httpserver` module since Java 9, backported as a stable internal API from Java 6). It requires **zero external JAR files**. The entire Java bridge compiles and runs with only:
- `pfcasync.jar` (J-Link, from PTC Creo installation)
- `jackson-databind`, `jackson-core`, `jackson-annotations` (JSON, three JARs)
- The JDK itself

No Maven, no Gradle, no Spring, no deployment descriptor. This makes the bridge trivially portable.

### Why Jackson over Gson?

**Jackson** (from FasterXML) is the most widely used Java JSON library. Compared to Gson:
- Jackson is ~3–5× faster for large payloads because it has a **streaming API** (`JsonParser` / `JsonGenerator`) that avoids building an in-memory tree.
- Jackson's `TypeReference<Map<String, Object>>{}` idiom handles generics at runtime correctly (Gson has edge cases with type erasure).
- Jackson's `SerializationFeature.INDENT_OUTPUT` gives pretty-printed JSON with one line of config.
- Jackson is the default for Spring Boot, Micronaut, Quarkus — knowing it is universally applicable.

### Why `httpx` over Python's `requests`?

**httpx** is a modern HTTP client for Python that offers:
- Per-request `timeout=` parameter (both connect timeout and read timeout together, or separately as `httpx.Timeout(connect=5.0, read=30.0)`).
- A `with httpx.Client() as c:` context manager that explicitly closes the TCP connection — no leaked sockets.
- Full async support (`httpx.AsyncClient`) if you ever need to convert the MCP server to async.
- `requests` is synchronous-only and uses urllib3 under the hood with less ergonomic timeout control.

---

## 2. The Java Side

### 2.1 `JsonUtil.java` — The Jackson Wrapper

**File**: [`util/JsonUtil.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/util/JsonUtil.java)

```java
private static final ObjectMapper mapper = new ObjectMapper();

static {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
}
```

**`ObjectMapper`** is Jackson's central class — it converts between Java objects and JSON strings. Declaring it `static final` means one instance is shared across the entire JVM lifetime. `ObjectMapper` is **thread-safe for reading and writing** after configuration, so sharing is correct and avoids the overhead of instantiating it per request.

The `static { }` block is a **static initializer** — it runs once when the class is first loaded by the JVM class loader, before any method is called. `SerializationFeature.INDENT_OUTPUT` tells Jackson to pretty-print JSON with newlines and spaces.

```java
public static Map<String, Object> parseMap(String json) {
    return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
}
```

`TypeReference<Map<String, Object>>() {}` — this anonymous subclass trick is how Jackson works around Java's **type erasure**. At runtime, Java normally erases generic type parameters (`List<String>` becomes `List`). By creating an anonymous subclass, the generic type is captured in the class's `getGenericSuperclass()` at compile time. Jackson reads it via reflection. Without this, Jackson would return a `LinkedHashMap<Object, Object>` with no type safety guarantee.

```java
private static String escape(String s) {
    return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
}
```

This **defensive JSON string escaping** prevents malformed JSON if an exception message itself contains a double-quote or newline. It is used only in the fallback error path when `writeValueAsString()` itself throws — a rare edge case.

---

### 2.2 `CreoSessionManager.java` — The J-Link Lifeline

**File**: [`CreoSessionManager.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/CreoSessionManager.java)

#### Singleton Pattern

```java
private static final CreoSessionManager instance = new CreoSessionManager();

private CreoSessionManager() { }

public static CreoSessionManager getInstance() {
    return instance;
}
```

The **Singleton** pattern ensures only one object of a class ever exists in a JVM. Here, `instance` is initialized with the class itself — this is called **Eager Initialization**. The JVM guarantees that static field initialization is thread-safe (protected by the class loader lock), so no `synchronized` block is needed here. We need exactly one connection to Creo. Two competing `AsyncConnection` objects pointed at the same Creo process would result in undefined behavior (J-Link is not designed for multi-client use).

#### `synchronized` — The JVM Monitor Lock

```java
public synchronized Map<String, Object> connect(int timeoutSeconds) { ... }
public synchronized boolean isConnected() { ... }
public synchronized Session getSession() throws Exception { ... }
```

The `synchronized` keyword on an instance method acquires the **intrinsic lock (monitor)** of `this` object before the method body executes and releases it on exit. In a concurrent environment — and we have a 4-thread HTTP server — two incoming requests could both call `getSession()` simultaneously. Without `synchronized`, both threads would find `isConnected() == false` at the same instant, both attempt `connect()`, and you'd end up with two racing J-Link connections and a corrupted state. `synchronized` serializes these calls: only one thread executes `getSession()` at a time.

#### Loading the Native Library — JNI

```java
public synchronized void ensureLibraryLoaded() {
    if (!libraryLoaded) {
        System.loadLibrary("pfcasyncmt");
        libraryLoaded = true;
    }
}
```

**JNI (Java Native Interface)** is the mechanism by which JVM bytecode calls C/C++ compiled code. `System.loadLibrary("pfcasyncmt")` tells the JVM to search the directories in the OS `PATH` environment variable for a file named `pfcasyncmt.dll` (Windows). It loads the DLL into the same process, links the native method stubs in `pfcasync.jar` to their C++ implementations inside the DLL, and then J-Link method calls cross the JNI boundary at every invocation.

The `libraryLoaded` boolean guard prevents calling `System.loadLibrary()` twice. Calling it a second time for the same library is a no-op in most JVMs, but the guard makes the intent explicit and avoids the overhead of the library lookup.

`UnsatisfiedLinkError` — the JVM throws this `Error` (not `Exception`) when `pfcasyncmt.dll` cannot be found in `PATH`. It is the single most common setup failure.

#### Connecting to Creo

```java
asyncConnection = pfcAsyncConnection.AsyncConnection_Connect(null, null, null, timeoutSeconds);
```

`AsyncConnection_Connect` is a J-Link static factory method. Its four parameters are:
1. `display` — X display string for UNIX (null on Windows)
2. `creo_exe_path` — path to `xtop.exe` (null = use whatever is already running)
3. `text_path` — path to Creo text directory (null = use defaults)
4. `timeoutSeconds` — how long to wait for `pro_comm_msg.exe` to relay the connection handshake

Passing all three as `null` means: **attach to an already-running Creo process** — do not launch a new one. This is the correct mode for an automation bridge that sits alongside a user's running Creo session.

```java
session = asyncConnection.GetSession();
String dir = session.GetCurrentDirectory();
result.put("connectionId", session.GetConnectionId());
```

After connection, `GetSession()` returns a `Session` object — the root handle for every J-Link API call. `GetCurrentDirectory()` returns Creo's working directory (where it looks for model files). `GetConnectionId()` returns a unique identifier for this J-Link session, useful for diagnostics.

#### Liveness Check

```java
public synchronized boolean isConnected() {
    if (asyncConnection == null || session == null) return false;
    try {
        return asyncConnection.IsRunning();
    } catch (Exception e) {
        asyncConnection = null;
        session = null;
        return false;
    }
}
```

`asyncConnection.IsRunning()` sends a lightweight ping to `pro_comm_msg.exe` (the IPC broker). If Creo has crashed or been closed, this call throws (or returns `false`), and we null out the connection fields. **Defensive nulling** on failure prevents `NullPointerException` on subsequent calls.

#### Auto-Reconnect in `getSession()`

```java
public synchronized Session getSession() throws Exception {
    if (!isConnected()) {
        Map<String, Object> connRes = connect(5);   // 5-second quick retry
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to Creo...");
        }
    }
    return session;
}
```

Every handler calls `mgr.getSession()` as its first step. The auto-reconnect with a 5-second timeout means that if Creo was briefly unavailable (e.g., restarting a model), the next API call will attempt to re-establish the link. If it fails, an `IllegalStateException` propagates up to `BaseHandler.handle()`, which catches it and returns a JSON error to the client.

#### `resolveModel()` — Flexible Model Lookup

```java
public synchronized Model resolveModel(String modelName) throws Exception {
    Session s = getSession();
    if (modelName == null || modelName.trim().isEmpty()) {
        return s.GetCurrentModel();   // active model fallback
    }
    Models models = s.ListModels();
    for (int i = 0; i < models.getarraysize(); i++) {
        Model m = models.get(i);
        if (m.GetFileName().equalsIgnoreCase(modelName) || m.GetFullName().equalsIgnoreCase(modelName)) {
            return m;
        }
    }
    return s.GetModelFromFileName(modelName);   // last-resort lookup
}
```

`s.ListModels()` returns a J-Link **CIP sequence** (`Models`) — PTC's proprietary collection type, not a Java `List`. It has `getarraysize()` and `get(int)` instead of `size()` and `get()`. The loop uses `equalsIgnoreCase()` for robustness — Creo filenames are case-insensitive on Windows. The fallback to `GetModelFromFileName()` handles the case where the model is loaded but not the current active model.

#### `formatCipException()` — CIP Error Decoding

```java
public static String formatCipException(jxthrowable jxe) {
    String msg = jxe.getMessage();
    String className = jxe.getClass().getSimpleName();
    return className + ": " + msg;
}
```

**`jxthrowable`** is PTC's base exception class for all J-Link errors. It extends Java's `Throwable`. Every J-Link API call can throw it. Common subclasses you encounter:
- `XStringTooLong` — a string exceeded a C-level fixed-length buffer (typically 31 chars for `ProName`)
- `XToolkitFound` — you tried to create something that already exists (e.g., a drawing with the same name)
- `XToolkitNotFound` — the requested object doesn't exist in the current session
- `XToolkitBadInputs` — wrong argument type or value passed to a J-Link method

---

### 2.3 `MainBridgeServer.java` — The HTTP Entry Point

**File**: [`MainBridgeServer.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/MainBridgeServer.java)

#### Binding the Server

```java
HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
```

`InetSocketAddress("127.0.0.1", port)` creates a socket address bound to the **loopback interface only**. Using `127.0.0.1` instead of `0.0.0.0` means the server is **not reachable from outside the machine** — a critical security decision. This bridge has no authentication; anyone who can reach port 9090 can control Creo. Binding to loopback ensures only processes on the same machine can connect.

The second argument (`0`) is the **TCP backlog** — the length of the kernel's queue for incoming connection requests that have not yet been `accept()`-ed. `0` tells the OS to choose its platform default (typically 50 on Windows). For a local-only server with low concurrency, any backlog value works.

#### Thread Pool

```java
server.setExecutor(Executors.newFixedThreadPool(4));
```

By default, `HttpServer` uses a single-threaded executor — one request at a time. `Executors.newFixedThreadPool(4)` creates a pool of exactly 4 worker threads that each pick up requests from a shared queue. **Why fixed and not cached?**  
- `newCachedThreadPool()` creates a new thread for every request and can grow unboundedly. Under a burst of 50 requests, 50 threads would all call J-Link simultaneously — which is not thread-safe.
- `newFixedThreadPool(4)` caps concurrency. Combined with `synchronized` methods on `CreoSessionManager`, this means at most 4 threads compete, but J-Link access is still serialized through the monitor lock. Requests beyond 4 queue up and wait.

#### Route Registration

```java
server.createContext("/api/model/active", new ModelHandler("active"));
server.createContext("/api/model/list",   new ModelHandler("list"));
server.createContext("/api/model/open",   new ModelHandler("open"));
```

`createContext(path, handler)` registers a prefix-based URL route. The **same handler class** (`ModelHandler`) handles multiple endpoints, distinguished by the `action` string passed to the constructor. This is a **Strategy-like pattern**: the object's behavior is parameterized at construction time. The alternative would be one class per endpoint — that would mean 18 handler classes instead of 6.

#### Background Auto-Connect

```java
new Thread(() -> {
    CreoSessionManager.getInstance().connect(10);
}).start();
```

This anonymous thread is not a **daemon thread** — it's a regular user thread. The JVM won't exit while it's running. However, `connect(10)` completes in ≤10 seconds, so it terminates quickly. The purpose is to pre-warm the J-Link connection: by the time the first real API call arrives, the session is already established.

#### Shutdown Endpoint

```java
server.createContext("/api/shutdown", new BaseHandler() {
    @Override
    protected Map<String, Object> execute(HttpExchange exchange) {
        new Thread(() -> {
            Thread.sleep(500);
            server.stop(0);
            System.exit(0);
        }).start();
        return JsonUtil.createSuccess("Shutting down bridge server...");
    }
});
```

The shutdown logic delays 500ms on a separate thread before calling `server.stop(0)`. This ensures the HTTP response (`"Shutting down..."`) is fully sent back to the client before the server socket closes. Without the delay, `server.stop(0)` could terminate the response mid-send.

---

### 2.4 `BaseHandler.java` — The Template Method

**File**: [`handler/BaseHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/BaseHandler.java)

**Abstract class** — `BaseHandler` cannot be instantiated directly. It implements `HttpHandler` (the `com.sun.net.httpserver` handler interface) and provides a **Template Method pattern**: `handle()` defines the algorithm skeleton (CORS → execute → JSON response), while subclasses override `execute()` to fill in the business logic.

#### CORS Headers

```java
exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
```

**CORS (Cross-Origin Resource Sharing)** is a browser security mechanism. A web page loaded from `https://claude.ai` cannot, by default, make `fetch()` calls to `http://127.0.0.1:9090` — they are different origins. The `Access-Control-Allow-Origin: *` response header tells the browser: "this server permits requests from any origin." Without these headers, browser-based AI UIs (anything using WebAssembly or a web front-end) would be blocked.

```java
if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
    exchange.sendResponseHeaders(204, -1);
    return;
}
```

An **OPTIONS preflight** is a request browsers automatically send before a cross-origin POST to ask the server what it allows. Returning HTTP `204 No Content` with `-1` (no body) satisfies the preflight and allows the real request to proceed.

#### Reading the Request Body

```java
ByteArrayOutputStream bos = new ByteArrayOutputStream();
byte[] buffer = new byte[2048];
int len;
while ((len = is.read(buffer)) > 0) {
    bos.write(buffer, 0, len);
}
String body = new String(bos.toByteArray(), StandardCharsets.UTF_8);
```

HTTP request bodies arrive as a raw `InputStream`. There is no `readAllBytes()` guarantee of a single call succeeding. The `while` loop with a 2 KB intermediate buffer is the correct streaming-read pattern. `StandardCharsets.UTF_8` ensures the bytes are interpreted as UTF-8, the universal encoding for JSON.

#### Sending the Response

```java
String json = JsonUtil.toJson(response);
byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
exchange.sendResponseHeaders(statusCode, bytes.length);
OutputStream os = exchange.getResponseBody();
os.write(bytes);
os.close();
```

`sendResponseHeaders(statusCode, contentLength)` must be called **before** writing to the body, and the `contentLength` must be exact. If you pass a wrong length, the HTTP response is malformed and clients may hang waiting for bytes that never come. Converting to `byte[]` first lets us measure the exact length before sending headers.

#### Query Parameter Parsing

```java
String query = uri.getQuery();   // e.g. "model=part.prt&pattern=DESC*"
String[] pairs = query.split("&");
for (String pair : pairs) {
    int idx = pair.indexOf("=");
    params.put(pair.substring(0, idx), pair.substring(idx + 1));
}
```

`com.sun.net.httpserver` does not have a built-in query-string parser (unlike Servlet API or JAX-RS). We split on `&` and then find the first `=` with `indexOf("=")` rather than `split("=")` because values can themselves contain `=` (e.g., Base64 strings). `substring(0, idx)` gets the key; `substring(idx + 1)` gets everything after.

---

### 2.5 `ConnectionHandler.java` — Session Control

**File**: [`handler/ConnectionHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/ConnectionHandler.java)

```java
public ConnectionHandler(String action) {
    this.action = action;
}
```

The `action` field is `final` (implicitly, since it's set in the constructor and never changed). This is the **command/action dispatch** approach: one class, parameterized behavior. In `execute()`, an `if-else` chain dispatches to the correct J-Link calls.

The `status` action builds a rich response that includes the active model's `filename`, `commonName`, `type`, and `isModified` flag — enough for an AI agent to understand the full Creo session state in one API call.

`disconnect()` calls `asyncConnection.Disconnect(2)` — the argument is a timeout in seconds to wait for any pending J-Link operations to complete before severing the connection. Passing `2` is a balance: long enough to let in-flight commands finish, short enough not to block shutdown.

---

### 2.6 `ModelHandler.java` — CAD Model Operations

**File**: [`handler/ModelHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/ModelHandler.java)

#### Opening a Model

```java
File f = new File(file);
if (f.isAbsolute() && f.getParent() != null) {
    session.ChangeDirectory(f.getParent());
    file = f.getName();
}
ModelDescriptor desc = pfcModel.ModelDescriptor_CreateFromFileName(file);
Model model = session.RetrieveModel(desc);
```

**`ModelDescriptor`** is J-Link's typed reference to a model file — it carries the model name, type (part/assembly/drawing), and version. `ModelDescriptor_CreateFromFileName()` creates one from a filename string. `session.RetrieveModel(desc)` loads the file into Creo's session (similar to `File → Open`). If the file is specified with an absolute path, we `ChangeDirectory` first so Creo's working directory context is correct.

#### `Display()` and `Window.Activate()`

```java
model.Display();
Window win = session.GetModelWindow(model);
if (win != null && activate) win.Activate();
```

`model.Display()` makes Creo open a graphics window for the model (equivalent to clicking a model in the session tree). `session.GetModelWindow(model)` returns the `Window` object (the UI frame). `win.Activate()` brings that window to the foreground and makes it the "active" model — important so subsequent API calls without a `model` argument target the right thing.

#### Regeneration

```java
if (model instanceof Solid) {
    Solid solid = (Solid) model;
    solid.Regenerate(null);
    model.Display();
}
```

**`instanceof`** checks the runtime type. In J-Link's type hierarchy, `Model` is the base interface; `Solid` (for `.prt` and `.asm` files) and `Drawing` (for `.drw` files) are specializations. Only `Solid` has a `Regenerate()` method. The `null` argument uses default regeneration options (full regen). After regeneration, `Display()` refreshes the graphics window.

#### `extractModelInfo()` — Safe Introspection

```java
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
```

Each J-Link getter is in its own `try`-catch block conceptually (here wrapping all of them together). `GetType()` returns a `ModelType` enum-like CIP constant — `.toString()` converts it to a readable string (`"MDL_PART"`, `"MDL_ASSEMBLY"`, `"MDL_DRAWING"`). `GetIsModified()` is the Creo equivalent of "unsaved changes" — the asterisk you see next to a filename in the title bar.

---

### 2.7 `ParameterHandler.java` — Data Attached to Geometry

**File**: [`handler/ParameterHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/ParameterHandler.java)

In Creo, **parameters** are named key-value pairs attached to a model (e.g., `MATERIAL = "STEEL"`, `WEIGHT = 2.45`). They appear in the model properties dialog, drive drawing title blocks (via relations), and feed into BOM reports.

#### Type Discriminant Pattern

```java
ParamValueType discr = val.Getdiscr();
int v = discr.getValue();
if (v == ParamValueType._PARAM_STRING) return "STRING";
if (v == ParamValueType._PARAM_DOUBLE) return "DOUBLE";
```

`ParamValue` is a **tagged union** — J-Link's equivalent of a discriminated union (like Rust's `enum` or C's `union` with a type tag). `Getdiscr()` returns the tag (`ParamValueType`), which tells you which getter to call (`GetStringValue()`, `GetDoubleValue()`, etc.). Calling the wrong getter (e.g., `GetDoubleValue()` on a STRING param) throws an exception.

#### Type Coercion in `createParamValue()`

```java
} else if ("DOUBLE".equals(t) || "FLOAT".equals(t) || "NUMBER".equals(t)) {
    double d = Double.parseDouble(val.toString());
    return pfcModelItem.CreateDoubleParamValue(d);
}
```

The `|| "FLOAT"` and `|| "NUMBER"` alternatives make the API forgiving — an AI agent might say `"type": "float"` when it means `"DOUBLE"`. The `Double.parseDouble(val.toString())` handles the case where `value` arrives from JSON as an integer (`42`) but the parameter type is DOUBLE: `.toString()` gives `"42"`, and `parseDouble` produces `42.0`. This is **defensive type coercion**.

#### Glob Pattern Matching

```java
private boolean matchesPattern(String str, String pattern) {
    String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
    return str.matches("(?i)" + regex);
}
```

Creo's native pattern language uses `*` (match any characters) and `?` (match one character) — the same as shell globs. Java's `String.matches()` uses regex. The conversion: escape literal dots (`"."` → `"\\."`), expand `*` to `.*`, expand `?` to `.`. The `(?i)` prefix makes the regex case-insensitive, matching Creo's own case-insensitive parameter name comparisons.

---

### 2.8 `FeatureHandler.java` — The Feature Tree

**File**: [`handler/FeatureHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/FeatureHandler.java)

In Creo Parametric, a **feature** is a single parametric operation in the build history: an Extrude, a Cut, a Hole, a Round, a Datum Plane. The ordered list of features is the **Feature Tree** (called the "Model Tree" in the UI). Every feature has:
- `GetId()` — unique integer ID within the model  
- `GetName()` — user-assigned name (e.g., "EXTRUDE_1")  
- `GetFeatTypeName()` — type string (e.g., `"PRO_FEAT_HOLE"`, `"PRO_FEAT_ROUND"`)  
- `GetStatus()` — `ACTIVE`, `SUPPRESSED`, `FAILED`, etc.

```java
if (!(model instanceof Solid)) {
    return JsonUtil.createError("Model '...' is not a Solid model (no features).");
}
Solid solid = (Solid) model;
Features feats = solid.ListFeaturesByType(null, null);
```

The `instanceof Solid` guard is important: Drawing models (`.drw`) don't have features. `ListFeaturesByType(null, null)` with both arguments null returns **all features** regardless of type. The optional `typeFilter` query parameter then applies client-side filtering (`equalsIgnoreCase`), avoiding a second J-Link call.

---

### 2.9 `DrawingHandler.java` — 2D Drawing Automation

**File**: [`handler/DrawingHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/DrawingHandler.java)

This is the most complex handler. It automates what normally requires a human to click through Creo's "New Drawing" wizard and then manually insert views and dimensions.

#### The 31-Character `ProName` Constraint

At the C++ kernel level, PTC uses `ProName` — a fixed-length wide-character array of 31 characters — for model identifiers. Any string passed to `CreateDrawingFromTemplate()` must fit. Names longer than 31 characters throw `XStringTooLong`. This is a fundamental constraint from Creo's 1980s-era FORTRAN/C heritage.

#### Template Staging — `prepareTemplate()`

```java
private String prepareTemplate(String template, String workingDir) {
    // 1. If absolute path exists, use it as source
    // 2. Check working dir, then installed Creo templates dir
    // 3. If found outside working dir, copy to working dir via java.nio.file.Files.copy()
    // 4. Return base name WITHOUT .drw extension and without path
}
```

`session.CreateDrawingFromTemplate()` resolves template names relative to Creo's **working directory** or its internal search path. If the template is elsewhere (e.g., `C:\Program Files\PTC\Creo 9.0.10.0\Common Files\templates\c_drawing.drw`), it must be copied to the working directory first. The method returns only the basename without extension (`"c_drawing"` not `"c_drawing.drw"`) because that's what `ProName` expects.

`java.nio.file.Files.copy(source.toPath(), dest.toPath())` — **NIO (New I/O)**, introduced in Java 7, is the modern file API. It uses OS-level file copy syscalls (more efficient than reading and writing byte-by-byte). The `!dest.exists()` check prevents re-copying if the template is already staged.

#### Creating the Drawing

```java
DrawingCreateOptions opts = DrawingCreateOptions.create();
opts.append(DrawingCreateOption.DRAWINGCREATE_SHOW_ERROR_DIALOG);

Drawing drawing = session.CreateDrawingFromTemplate(drawingName, templateName, modelDesc, opts);
```

`DrawingCreateOptions` is an **option set builder** — you create it, append flags, then pass it. `DRAWINGCREATE_SHOW_ERROR_DIALOG` tells Creo to show its own error dialog if the creation fails (useful during development so you see Creo-level error messages, not just `null`).

The try/catch with alt-name retry handles the ambiguity of whether Creo expects `"c_drawing"` or `"c_drawing.drw"`:

```java
try {
    drawing = session.CreateDrawingFromTemplate(drawingName, templateName, modelDesc, opts);
} catch (jxthrowable jxe) {
    String altName = templateName.endsWith(".drw")
        ? templateName.substring(0, templateName.length() - 4)
        : (templateName + ".drw");
    drawing = session.CreateDrawingFromTemplate(drawingName, altName, modelDesc, opts);
}
```

This is a **retry with alternate format** strategy, common when working with APIs that have inconsistent input format expectations.

#### Showing Dimensions — The Projection Engine

```java
Drawing drawing = (Drawing) mgr.resolveModel(drawingName);
View2Ds views = drawing.List2DViews();
int viewCount = views.getarraysize();
```

`List2DViews()` returns all the views placed on the drawing sheet (Front view, Top view, Isometric, etc.) as a CIP sequence `View2Ds`. Each `View2D` represents a viewport projected from the 3D model.

```java
Features feats = solid.ListFeaturesByType(null, null);
for (int i = 0; i < count; i++) {
    Feature f = feats.get(i);
    ModelItems dimItems = f.ListSubItems(ModelItemType.ITEM_DIMENSION);
    // ...
}
```

The dimension discovery traverses the **feature tree**: for each feature, `ListSubItems(ModelItemType.ITEM_DIMENSION)` returns the dimensions that define that feature (e.g., an Extrude has a depth dimension; a Hole has a diameter). This recursive traversal collects all model dimensions.

A `HashMap<Integer, BaseDimension>` keyed by dimension ID deduplicates dimensions that appear in multiple features.

```java
DrawingDimensionShowInstructions inst =
    pfcView2D.DrawingDimensionShowInstructions_Create(view, (ComponentPath) null);
dim.Show(inst);
```

`DrawingDimensionShowInstructions_Create` is a **factory method** — it creates the instruction object that tells Creo *where* to show the dimension (on a specific `View2D`, for all component instances since `ComponentPath` is `null`). `dim.Show(inst)` projects the 3D dimension annotation onto the 2D view. If the dimension's geometry isn't visible in that view (e.g., a depth dimension on a top view), it throws an exception, which is caught and the next view is tried.

The view selection logic preferentially skips isometric views for dimension placement:

```java
if (vName != null && vName.toLowerCase().contains("iso") && viewCount > 1) {
    continue;  // skip isometric views, prefer orthographic
}
```

Isometric views are harder to read with dimensions; orthographic views (Front, Top, Right) are the engineering standard. If no orthographic view accepts the dimension, the fallback `dim.Show(null)` lets Creo choose the view automatically.

---

### 2.10 `ExportHandler.java` — STEP and PDF Export

**File**: [`handler/ExportHandler.java`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/java-bridge/src/com/creomcp/bridge/handler/ExportHandler.java)

#### STEP Export

**STEP (Standard for the Exchange of Product Data)** — ISO 10303 — is the universal 3D CAD interchange format. Every major CAD system reads and writes STEP. Our code uses J-Link's `ExportFile` instructions for STEP geometry export.

#### PDF Export

```java
pfcExport.PDFExportInstructions_Create()
```

`PDFExportInstructions_Create()` is another factory method — it returns a PDF export instruction object. The critical distinction:
- For a **Drawing** (`.drw`) model: `PDFExportMode._PDF_2D_DRAWING` — produces a vector PDF with crisp lines, exactly like printing the drawing.
- For a **Solid** (`.prt` / `.asm`) model: a raster or shaded 3D view PDF.

```java
model.Export(filename, instructions);
```

`model.Export()` is **polymorphic** — the same method call works on both `Drawing` and `Solid` because both implement the `Model` interface's `Export()` method. The instructions object's type determines the export behavior. This is the **Open/Closed Principle** in action: the export mechanism is open for extension (new instruction types) without modifying the caller.

---

## 3. The Python Side

### 3.1 `client.py` — The HTTP Bridge Client

**File**: [`mcp-server/creo_mcp/client.py`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/mcp-server/creo_mcp/client.py)

#### `httpx.Client` as a Context Manager

```python
def _get(self, path: str, params=None) -> Dict[str, Any]:
    url = f"{self.base_url}{path}"
    with httpx.Client(timeout=10.0) as client:
        res = client.get(url, params=params)
        res.raise_for_status()
        return res.json()
```

`with httpx.Client(...) as client:` is a **context manager** — Python's equivalent of Java's `try-with-resources`. When the `with` block exits (normally or via exception), `client.__exit__()` is called, which closes the TCP connection and releases the socket. Without the context manager, HTTP connections can leak — particularly important when the MCP server is a long-lived process.

`timeout=10.0` sets the combined connect + read timeout to 10 seconds for GET requests. POST requests use `timeout=30.0` — long-running Creo operations (regeneration, drawing creation) can take 10–20 seconds.

`res.raise_for_status()` — **fail-fast** pattern. If the Java bridge returns HTTP 4xx or 5xx, this raises `httpx.HTTPStatusError` immediately. Without it, you'd get a success-looking `res.json()` containing `{"success": false, "error": "..."}` and the error might be silently swallowed.

`httpx.ConnectError` — the specific exception when the TCP connection is refused (bridge not running). Catching it separately from the generic `Exception` allows a precise, actionable error message: "Ensure run_bridge.bat is running."

#### f-strings

```python
url = f"{self.base_url}{path}"
```

Python **f-strings** (formatted string literals, introduced in Python 3.6) embed expressions directly in string literals using `{}`. They are faster than `.format()` and more readable than `%` formatting.

#### Type Hints

```python
def _get(self, path: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
```

Python type hints (PEP 484) are **not enforced at runtime** by the Python interpreter — they are annotations for static analysis tools (mypy, Pyright) and for documentation. `Optional[X]` is equivalent to `Union[X, None]` — the parameter can be either type `X` or `None`. `Dict[str, Any]` means a dictionary with string keys and values of any type.

The MCP SDK reads these type annotations using Python's `inspect` module to generate JSON Schema for tool manifests.

#### Conditional Body Building

```python
def set_parameter(self, name, value, param_type=None, model_name=None, description=None):
    data: Dict[str, Any] = {"name": name, "value": value}
    if param_type:
        data["type"] = param_type
    if model_name:
        data["model"] = model_name
    if description:
        data["description"] = description
    return self._post("/api/parameter/set", data)
```

Only including keys that have values is **minimal payload construction** — the Java handler uses `body.get("type")` which returns `null` if the key is absent, allowing server-side type inference. Sending `"type": null` would also work but is less clean.

---

### 3.2 `server.py` — The MCP Tool Layer

**File**: [`mcp-server/creo_mcp/server.py`](file:///C:/Users/tanan/CREO_MCP/creo_mcp/mcp-server/creo_mcp/server.py)

#### MCP — Model Context Protocol

**MCP (Model Context Protocol)** is an open standard (introduced by Anthropic) that defines how AI language models communicate with external tools and data sources. It uses **JSON-RPC 2.0** as the message format and **stdio** (standard input/output) as the transport when running as a subprocess. When Claude Desktop launches `python -m creo_mcp.server`, it communicates by writing JSON frames to the process's stdin and reading responses from stdout.

#### `MCPServer`

```python
from mcp.server.mcpserver import MCPServer
mcp = MCPServer("creo-mcp")
```

`MCPServer("creo-mcp")` creates the server instance with the name `"creo-mcp"` — this name appears in Claude's UI as the tool provider. The MCP SDK 2.x renamed the API from the legacy `FastMCP` class to `MCPServer`. Importing from `mcp.server.mcpserver` is the correct 2.x path.

#### `@mcp.tool()` — Auto Schema Generation

```python
@mcp.tool()
def creo_get_status() -> str:
    """Check whether the bridge is connected to PTC Creo Parametric..."""
    res = client.get_status()
    return json.dumps(res, indent=2)
```

The `@mcp.tool()` decorator does three things at registration time:
1. Reads the function's **signature** (parameter names and type annotations) using Python's `inspect.signature()`.
2. Reads the **docstring** as the tool description.
3. Generates a **JSON Schema** object from the type annotations (e.g., `str` → `{"type": "string"}`, `bool` → `{"type": "boolean"}`).

This schema is what Claude receives when it asks "what tools are available?" — it tells Claude the tool name, description, and what arguments it accepts.

#### Return Type: Always `str`

```python
return json.dumps(res, indent=2)
```

MCP tool return values must be **strings**. The AI model receives the return value as text and reads it. `json.dumps(res, indent=2)` serializes the Python dict (which came from `res.json()` via httpx, which deserialized the Java bridge's JSON response) back to a pretty-printed JSON string. This round-trip (Java → JSON → httpx → Python dict → json.dumps → string → Claude) is the full data path.

#### `mcp.run()` — The stdio Event Loop

```python
def main():
    mcp.run()
```

`mcp.run()` starts an **async event loop** that:
1. Reads complete JSON-RPC 2.0 frames from `sys.stdin` (length-prefixed or newline-delimited depending on MCP version).
2. Dispatches each `tools/call` request to the decorated function.
3. Writes the result back to `sys.stdout`.

Claude Desktop manages the process lifecycle: it spawns the Python process when the server is first needed and can restart it on failure.

#### Tool Naming Convention

All tools are prefixed with `creo_` (e.g., `creo_get_status`, `creo_open_model`). This **namespacing convention** avoids collisions when Claude has multiple MCP servers registered — it's clear at a glance that `creo_export_pdf` belongs to this bridge.

---

## 4. The Build System

### `javac --release 17` vs `--source`/`--target`

```bat
javac --release 17 -d bin -cp "lib\*" src\com\creomcp\bridge\...
```

`--release 17` is a single flag that sets three things simultaneously:
1. **Source version**: Java 17 syntax is accepted.
2. **Target bytecode version**: Output `.class` files use Java 17 bytecode format (class file major version 61.0).
3. **Boot classpath**: The compiler checks your code against the Java 17 standard library API, preventing accidental use of APIs added in Java 18+.

Using `--source 17 --target 17` separately does NOT set the boot classpath — you could accidentally reference Java 21 APIs and get `NoSuchMethodError` at runtime on a Java 17 JVM. `--release` is always preferred for correctness.

**Why Java 17?** PTC's `pfcasync.jar` ships compiled for Java 11 minimum. Java 17 is the latest LTS (Long Term Support) release as of 2024, widely available, and forward-compatible.

### `run_bridge.bat` — PATH Manipulation

```bat
set PATH=C:\Program Files\PTC\Creo 9.0.10.0\bin\;%PATH%
set PATH=C:\Program Files\PTC\Creo 9.0.10.0\Common Files\lib\obj\;%PATH%
java -cp "bin;lib\*" com.creomcp.bridge.MainBridgeServer
```

The Creo installation places `pfcasyncmt.dll` in `Common Files\lib\obj\`. The JVM's `System.loadLibrary("pfcasyncmt")` searches the directories listed in the `PATH` environment variable (on Windows). By prepending the Creo lib directory to `PATH` at the start of the batch file, we guarantee the correct DLL is found before the JVM starts — without modifying the system-wide PATH permanently.

`-cp "bin;lib\*"` — the **classpath** tells Java where to find compiled `.class` files (`bin\`) and library JARs (`lib\*` — the glob expands to all `.jar` files in `lib\`).

---

## 5. The IPC / OS Layer

### `pro_comm_msg.exe` — The IPC Broker

When J-Link connects to Creo, the actual inter-process communication doesn't go directly JVM↔Creo. Instead:

```
JVM (our bridge)
    ↕  J-Link protocol (via pfcasyncmt.dll / JNI)
pro_comm_msg.exe  (PTC's IPC message router)
    ↕  Creo IPC protocol (named pipes / shared memory)
xtop.exe  (the actual Creo Parametric process)
```

`pro_comm_msg.exe` is a small PTC-provided executable that acts as a **message broker**: it multiplexes J-Link API calls from multiple clients, serializes them into Creo's internal command queue, and routes responses back. You don't start it manually — `AsyncConnection_Connect()` launches it automatically from the Creo `bin\` directory (which is why that directory must be in `PATH`).

### `pfcasync.jar` vs `pfcasyncmt.dll`

- **`pfcasync.jar`**: Pure Java classes — the J-Link API surface (interfaces, factory methods, constants). Compiled code running on the JVM.
- **`pfcasyncmt.dll`**: Native C++ implementation — the actual code that communicates with `pro_comm_msg.exe`. "mt" = multi-threaded variant.

They are designed as a pair. The `jar` declares `native` methods; the DLL implements them.

---

## 6. Error Taxonomy — What Each Exception Means

| Exception | J-Link / Java | Meaning | Typical Fix |
|-----------|--------------|---------|-------------|
| `UnsatisfiedLinkError` | Java | `pfcasyncmt.dll` not in PATH | Add Creo `lib\obj` to PATH |
| `XStringTooLong` | J-Link | Model/template name > 31 chars | Shorten name or stage to working dir |
| `XToolkitFound` | J-Link | Drawing/model name already exists | Use unique names; add timestamp suffix |
| `XToolkitNotFound` | J-Link | Model/feature not in session | Verify model is open in Creo |
| `XToolkitBadInputs` | J-Link | Wrong argument to J-Link API | Check API documentation for param types |
| `IllegalStateException` | Our code | `getSession()` called when not connected | Call `/api/connect` first |
| `httpx.ConnectError` | Python | Port 9090 not listening | Start `run_bridge.bat` |
| `ModuleNotFoundError` | Python | Wrong Python env / missing package | Use full Python path in MCP config |
| `UnsupportedClassVersionError 69.0` | Java | `.class` file built with Java 25, run on Java 17 | Recompile with `--release 17` |

---

## 7. Keyword Glossary

| Term | What It Is | Why It Matters Here |
|------|-----------|---------------------|
| **J-Link** | PTC's proprietary Java SDK for Creo automation. Distributed as `pfcasync.jar` + `pfcasyncmt.dll`. | The only way to programmatically control Creo from code. |
| **JNI (Java Native Interface)** | JVM mechanism to call native C/C++ code from Java. | `pfcasyncmt.dll` is C++; `System.loadLibrary()` bridges to it. |
| **CIP (Common Interface Platform)** | PTC's internal architecture layer that J-Link exposes. All J-Link types live in `com.ptc.*` packages. | Understanding CIP explains why J-Link collections use `getarraysize()` instead of `size()`. |
| **MCP (Model Context Protocol)** | Open standard by Anthropic for AI ↔ tool communication. JSON-RPC 2.0 over stdio. | How Claude Desktop calls our Python server. |
| **JSON-RPC 2.0** | A simple remote procedure call protocol encoded as JSON. Has `method`, `params`, `id` fields. | The wire format Claude uses to invoke MCP tools. |
| **Singleton Pattern** | A design pattern ensuring one and only one instance of a class. | `CreoSessionManager` — one J-Link session globally. |
| **Template Method Pattern** | Abstract base class defines algorithm skeleton; subclasses fill in steps. | `BaseHandler.handle()` calls abstract `execute()`. |
| **Synchronized** | Java keyword acquiring the object's monitor lock — only one thread can execute the method at a time. | Prevents concurrent J-Link calls from corrupting the session state. |
| **ProName** | C-level fixed-width wide-char buffer (31 chars) for model identifiers in Creo's kernel. | Causes `XStringTooLong` if exceeded; drives the template staging logic. |
| **STEP (ISO 10303)** | Universal 3D CAD exchange format. | Platform-neutral output — any CAD system reads `.stp` files. |
| **CORS** | Browser security policy requiring servers to explicitly allow cross-origin requests. | Needed for browser-based AI UIs to call our localhost HTTP server. |
| **Preflight (OPTIONS)** | Browser-initiated request asking the server what cross-origin actions are allowed. | Our `BaseHandler` returns `204` to satisfy preflight. |
| **TypeReference** | Jackson idiom to preserve generic type information past Java type erasure. | Required for `mapper.readValue(json, new TypeReference<Map<String,Object>>(){})`. |
| **Type Erasure** | Java runtime removes generic type parameters — `List<String>` becomes `List` at runtime. | Why `TypeReference` anonymous subclass trick is needed. |
| **Context Manager** | Python `with` statement resource management — calls `__exit__` on block exit. | Ensures httpx TCP connections are closed after each request. |
| **f-string** | Python 3.6+ string interpolation: `f"hello {name}"`. | Used throughout `client.py` for URL building. |
| **`Optional[X]`** | Python type hint for `Union[X, None]` — value may be absent. | Used in `client.py` signatures; MCP SDK uses it to generate optional JSON Schema fields. |
| **`@mcp.tool()`** | MCP SDK decorator that registers a Python function as an AI-callable tool. | Reads signature + docstring to auto-generate the tool's JSON Schema manifest. |
| **`--release 17`** | Java compiler flag setting source, target, AND boot classpath together. | Safer than `--source`/`--target` pair for cross-version compilation. |
| **NIO (`java.nio`)** | Java's New I/O API — OS-native file operations, non-blocking I/O. | `Files.copy()` for template staging is more efficient than byte-by-byte copy. |
| **Daemon thread** | A JVM thread that does not prevent JVM exit when all non-daemon threads are done. | Used in auto-connect background threads so the JVM can shut down cleanly. |
| **Fixed Thread Pool** | `Executors.newFixedThreadPool(N)` — exactly N threads, excess requests queue. | Caps J-Link concurrency; prevents unbounded thread growth under load. |
| **`raise_for_status()`** | httpx method that raises `HTTPStatusError` on 4xx/5xx responses. | Fail-fast: errors from the Java bridge surface immediately. |
| **`jxthrowable`** | PTC's base class for all J-Link runtime errors. Extends Java `Throwable`. | Every J-Link API call can throw it; must always be caught explicitly. |
| **`ModelDescriptor`** | J-Link typed reference to a model file — carries name, type, version. | Required by `RetrieveModel()` and `CreateDrawingFromTemplate()`. |
| **`ParamValueType`** | J-Link discriminant enum for the type of a `ParamValue` (STRING/DOUBLE/INTEGER/BOOLEAN). | Must call the matching getter or risk exceptions. |
| **`DrawingDimensionShowInstructions`** | J-Link instruction object for projecting 3D dimensions onto a 2D view. | Core of the dimension automation — without it, drawings are blank. |
| **`View2D`** | J-Link object representing a single view (Front, Top, Isometric) on a drawing sheet. | Target for dimension projection; isometric views are deprioritized. |
| **`ComponentPath`** | J-Link object identifying an assembly instance path. `null` = all instances. | Passed to `DrawingDimensionShowInstructions_Create` to show dims for all occurrences. |

---

*End of Build Reference. Total stack: Java 17 → J-Link 9.0 → com.sun.net.httpserver → HTTP/1.1 → httpx → Python 3.14 → MCP SDK 2.x → Claude.*

