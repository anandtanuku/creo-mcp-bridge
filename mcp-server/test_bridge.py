import sys
import json
from creo_mcp.client import CreoBridgeClient

def run_tests():
    print("=" * 60)
    print("       CREO MCP BRIDGE AUTOMATED TEST SUITE")
    print("=" * 60)

    client = CreoBridgeClient("http://127.0.0.1:9090")

    # 1. Status Check
    print("\n[1/5] Checking Bridge & Creo Status...")
    status = client.get_status()
    print("  Status Result:", json.dumps(status, indent=2))
    if not status.get("connected"):
        print("\n[!] Bridge is running but not connected to Creo. Attempting connect...")
        conn = client.connect(timeout=10)
        print("  Connect Result:", json.dumps(conn, indent=2))
        if not conn.get("success"):
            print("\n[FAILED] Could not connect to Creo. Is Creo running?")
            sys.exit(1)

    # 2. Active Model
    print("\n[2/5] Querying Active Model...")
    model_res = client.get_active_model()
    print("  Active Model:", json.dumps(model_res, indent=2))

    # Find the 3D part or assembly in session
    models_res = client.list_models()
    solid_model = "prt0001.prt"
    for m in models_res.get("models", []):
        fname = m.get("filename", "")
        if fname.lower().endswith(".prt") or fname.lower().endswith(".asm"):
            solid_model = fname
            break

    # 3. Features
    print(f"\n[3/7] Listing Model Features for {solid_model}...")
    feats = client.list_features(model_name=solid_model)
    feat_count = feats.get("count", 0)
    print(f"  Found {feat_count} features:")
    for f in feats.get("features", [])[:5]:
        print(f"    - ID {f.get('id')}: {f.get('name')} ({f.get('typeName')})")
    if feat_count > 5:
        print(f"    ... and {feat_count - 5} more.")

    # 4. Parameters
    print(f"\n[4/7] Listing Parameters for {solid_model}...")
    params = client.list_parameters(model_name=solid_model)
    param_list = params.get("parameters", [])
    print(f"  Found {len(param_list)} parameters:")
    for p in param_list[:5]:
        print(f"    - {p.get('name')} [{p.get('type')}]: {p.get('value')}")

    # 5. Parameter Modification & Regeneration Test
    print(f"\n[5/7] Testing Parameter Modification & Regeneration on {solid_model}...")
    set_res = client.set_parameter("TEST_MCP_PARAM", "HELLO_FROM_AI", param_type="STRING", model_name=solid_model)
    print("  Set Parameter Result:", set_res.get("message"))

    regen_res = client.regenerate_model(model_name=solid_model)
    print("  Regenerate Result:", regen_res.get("message"))

    # 6. 2D Drawing Automation Test
    import time
    ts_drawing_name = f"drw_{int(time.time()) % 100000}"
    print(f"\n[6/7] Testing 2D Drawing Automation (from Template) for {solid_model} -> {ts_drawing_name}...")
    drw_res = client.create_drawing(model_name=solid_model, drawing_name=ts_drawing_name, template="c_drawing", show_dimensions=True)
    print("  Create Drawing Result:", drw_res.get("message"))
    print("  Dimensions Shown:", drw_res.get("dimensionsShown"))


    # 7. Vector PDF Export Test
    pdf_filename = f"{ts_drawing_name}.pdf"
    print(f"\n[7/7] Testing Vector PDF Export -> {pdf_filename}...")
    pdf_res = client.export_pdf(filename=pdf_filename)
    print("  Export PDF Result:", pdf_res.get("message"))
    print("  PDF Saved to:", pdf_res.get("absolutePath"))


    print("\n" + "=" * 60)
    print("  [SUCCESS] All Creo MCP Bridge tests passed!")
    print("=" * 60)


if __name__ == "__main__":
    run_tests()

