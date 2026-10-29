import json
from typing import Optional
from mcp.server.mcpserver import MCPServer
from creo_mcp.client import CreoBridgeClient

# Initialize MCP Server
mcp = MCPServer("creo-mcp")
client = CreoBridgeClient("http://127.0.0.1:9090")


@mcp.tool()
def creo_get_status() -> str:
    """Check whether the bridge is connected to PTC Creo Parametric and return active session details."""
    res = client.get_status()
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_connect(timeout_seconds: int = 30) -> str:
    """Connect to the running PTC Creo Parametric session via J-Link.
    
    Args:
        timeout_seconds: Maximum time in seconds to wait for connection handshake (default: 30).
    """
    res = client.connect(timeout=timeout_seconds)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_disconnect() -> str:
    """Disconnect from the active PTC Creo Parametric session."""
    res = client.disconnect()
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_get_active_model() -> str:
    """Retrieve details about the currently active/focused CAD model in Creo (filename, type, common name, modified flag)."""
    res = client.get_active_model()
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_list_models() -> str:
    """List all CAD models currently loaded in the Creo session."""
    res = client.list_models()
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_open_model(file_path: str, display: bool = True, activate: bool = True) -> str:
    """Open and display a CAD model (.prt, .asm, .drw) in Creo.
    
    Args:
        file_path: Absolute file path or filename within the Creo working directory.
        display: Whether to display the model in a Creo graphics window (default: True).
        activate: Whether to make the model window the active window (default: True).
    """
    res = client.open_model(file_path=file_path, display=display, activate=activate)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_save_model(model_name: str = "") -> str:
    """Save a CAD model to disk.
    
    Args:
        model_name: Filename of the model to save (e.g. 'part.prt'). If omitted, saves the currently active model.
    """
    res = client.save_model(model_name=model_name or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_regenerate_model(model_name: str = "") -> str:
    """Regenerate a solid model in Creo to update geometry after parameter or dimension modifications.
    
    Args:
        model_name: Name of the model to regenerate. If omitted, regenerates the active model.
    """
    res = client.regenerate_model(model_name=model_name or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_list_parameters(model_name: str = "", name_pattern: str = "") -> str:
    """List all parameters on a Creo model.
    
    Args:
        model_name: Name of the model (e.g. 'part.prt'). If omitted, targets the active model.
        name_pattern: Optional glob pattern to filter parameter names (e.g. 'DESC*', 'LENGTH?').
    """
    res = client.list_parameters(model_name=model_name or None, pattern=name_pattern or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_get_parameter(name: str, model_name: str = "") -> str:
    """Get the value, type, and designation status of a specific parameter.
    
    Args:
        name: Name of the parameter to query.
        model_name: Target model name. If omitted, queries the active model.
    """
    res = client.get_parameter(name=name, model_name=model_name or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_set_parameter(
    name: str,
    value: str,
    param_type: str = "STRING",
    model_name: str = "",
    description: str = "",
) -> str:
    """Set or create a parameter on a Creo CAD model.
    
    Args:
        name: Name of the parameter (e.g. 'MATERIAL', 'TOTAL_LENGTH').
        value: Value to set (as string, will be coerced based on param_type).
        param_type: Data type: 'STRING', 'DOUBLE', 'INTEGER', or 'BOOLEAN' (default: 'STRING').
        model_name: Target model name. If omitted, targets the active model.
        description: Optional parameter description string.
    """
    res = client.set_parameter(
        name=name,
        value=value,
        param_type=param_type,
        model_name=model_name or None,
        description=description or None,
    )
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_delete_parameter(name: str, model_name: str = "") -> str:
    """Delete a parameter from a Creo CAD model.
    
    Args:
        name: Name of the parameter to delete.
        model_name: Target model name. If omitted, targets the active model.
    """
    res = client.delete_parameter(name=name, model_name=model_name or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_list_features(model_name: str = "", feature_type: str = "") -> str:
    """List features (extrusions, cuts, holes, datum planes, etc.) in a Creo solid model.
    
    Args:
        model_name: Target model name. If omitted, targets the active model.
        feature_type: Optional filter by feature type name (e.g. 'PRO_FEAT_HOLE', 'PRO_FEAT_DATUM').
    """
    res = client.list_features(model_name=model_name or None, feature_type=feature_type or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_export_step(filename: str = "", model_name: str = "") -> str:
    """Export a Creo 3D model to a STEP file (.stp).
    
    Args:
        filename: Destination STEP file name or path (e.g. 'my_part.stp'). If omitted, defaults to <model_name>.stp.
        model_name: Target model name. If omitted, exports the active model.
    """
    res = client.export_step(filename=filename or None, model_name=model_name or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_export_pdf(filename: str = "", model_name: str = "") -> str:
    """Export an active Creo drawing (.drw) or 3D model to high-quality vector PDF.
    
    Args:
        filename: Destination PDF file name or path (e.g. 'part_drawing.pdf'). If omitted, defaults to <model_name>.pdf.
        model_name: Target model or drawing name. If omitted, exports the currently active window.
    """
    res = client.export_pdf(filename=filename or None, model_name=model_name or None)
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_create_drawing(
    model_name: str = "",
    drawing_name: str = "",
    template: str = "",
    display: bool = True,
    activate: bool = True,
    show_dimensions: bool = True,
) -> str:
    """Automate the generation of a 2D engineering drawing (.drw) from a 3D model using a Creo drawing template.
    Standard views (Front, Top, Isometric), title block parameters, and feature dimensions are automatically placed.
    
    Args:
        model_name: Target 3D model name (e.g. 'prt0001.prt'). If omitted, uses the active model.
        drawing_name: Name of the drawing to create (e.g. 'bracket_drawing'). If omitted, defaults to <model_name>_drw.
        template: Template drawing file name or path (e.g. 'c_drawing.drw', 'a3_drawing.drw'). If omitted, defaults to standard 'c_drawing.drw'.
        display: Whether to open and display the new drawing in a Creo window (default: True).
        activate: Whether to activate the drawing window as the active model (default: True).
        show_dimensions: Whether to automatically project and show 3D model dimensions onto the drawing views (default: True).
    """
    res = client.create_drawing(
        model_name=model_name or None,
        drawing_name=drawing_name or None,
        template=template or None,
        display=display,
        activate=activate,
        show_dimensions=show_dimensions,
    )
    return json.dumps(res, indent=2)


@mcp.tool()
def creo_show_drawing_dimensions(
    drawing_name: str = "",
    model_name: str = "",
) -> str:
    """Show and display 3D model annotations/dimensions onto the views of an active or specified 2D drawing (.drw).
    
    Args:
        drawing_name: Name of the drawing (.drw) to display dimensions on. If omitted, uses the active drawing.
        model_name: Name of the underlying 3D model (.prt or .asm). If omitted, auto-detected from the drawing.
    """
    res = client.show_drawing_dimensions(
        drawing_name=drawing_name or None,
        model_name=model_name or None,
    )
    return json.dumps(res, indent=2)




def main():
    mcp.run()


if __name__ == "__main__":
    main()

