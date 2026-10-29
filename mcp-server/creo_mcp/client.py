import json
from typing import Any, Dict, Optional
import httpx


class CreoBridgeClient:
    """Client for communicating with the Creo MCP Java Bridge HTTP API."""

    def __init__(self, base_url: str = "http://127.0.0.1:9090"):
        self.base_url = base_url.rstrip("/")

    def _get(self, path: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = f"{self.base_url}{path}"
        try:
            with httpx.Client(timeout=10.0) as client:
                res = client.get(url, params=params)
                res.raise_for_status()
                return res.json()
        except httpx.ConnectError:
            return {
                "success": False,
                "error": True,
                "message": (
                    f"Could not connect to Creo Java Bridge at {self.base_url}. "
                    "Ensure run_bridge.bat is running."
                ),
            }
        except Exception as e:
            return {"success": False, "error": True, "message": str(e)}

    def _post(self, path: str, json_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = f"{self.base_url}{path}"
        try:
            with httpx.Client(timeout=30.0) as client:
                res = client.post(url, json=json_data or {})
                res.raise_for_status()
                return res.json()
        except httpx.ConnectError:
            return {
                "success": False,
                "error": True,
                "message": (
                    f"Could not connect to Creo Java Bridge at {self.base_url}. "
                    "Ensure run_bridge.bat is running."
                ),
            }
        except Exception as e:
            return {"success": False, "error": True, "message": str(e)}

    # Connection API
    def get_status(self) -> Dict[str, Any]:
        return self._get("/api/status")

    def connect(self, timeout: int = 30) -> Dict[str, Any]:
        return self._post("/api/connect", {"timeout": timeout})

    def disconnect(self) -> Dict[str, Any]:
        return self._post("/api/disconnect")

    # Model API
    def get_active_model(self) -> Dict[str, Any]:
        return self._get("/api/model/active")

    def list_models(self) -> Dict[str, Any]:
        return self._get("/api/model/list")

    def open_model(self, file_path: str, display: bool = True, activate: bool = True) -> Dict[str, Any]:
        return self._post("/api/model/open", {
            "file": file_path,
            "display": display,
            "activate": activate,
        })

    def save_model(self, model_name: Optional[str] = None) -> Dict[str, Any]:
        data = {}
        if model_name:
            data["model"] = model_name
        return self._post("/api/model/save", data)

    def regenerate_model(self, model_name: Optional[str] = None) -> Dict[str, Any]:
        data = {}
        if model_name:
            data["model"] = model_name
        return self._post("/api/model/regenerate", data)

    # Parameter API
    def list_parameters(self, model_name: Optional[str] = None, pattern: Optional[str] = None) -> Dict[str, Any]:
        params = {}
        if model_name:
            params["model"] = model_name
        if pattern:
            params["pattern"] = pattern
        return self._get("/api/parameter/list", params=params)

    def get_parameter(self, name: str, model_name: Optional[str] = None) -> Dict[str, Any]:
        params = {"name": name}
        if model_name:
            params["model"] = model_name
        return self._get("/api/parameter/get", params=params)

    def set_parameter(
        self,
        name: str,
        value: Any,
        param_type: Optional[str] = None,
        model_name: Optional[str] = None,
        description: Optional[str] = None,
    ) -> Dict[str, Any]:
        data: Dict[str, Any] = {"name": name, "value": value}
        if param_type:
            data["type"] = param_type
        if model_name:
            data["model"] = model_name
        if description:
            data["description"] = description
        return self._post("/api/parameter/set", data)

    def delete_parameter(self, name: str, model_name: Optional[str] = None) -> Dict[str, Any]:
        data = {"name": name}
        if model_name:
            data["model"] = model_name
        return self._post("/api/parameter/delete", data)

    # Feature API
    def list_features(self, model_name: Optional[str] = None, feature_type: Optional[str] = None) -> Dict[str, Any]:
        params = {}
        if model_name:
            params["model"] = model_name
        if feature_type:
            params["type"] = feature_type
        return self._get("/api/feature/list", params=params)

    # Export API
    def export_step(self, filename: Optional[str] = None, model_name: Optional[str] = None) -> Dict[str, Any]:
        data = {}
        if filename:
            data["filename"] = filename
        if model_name:
            data["model"] = model_name
        return self._post("/api/export/step", data)

    def export_pdf(self, filename: Optional[str] = None, model_name: Optional[str] = None) -> Dict[str, Any]:
        data = {}
        if filename:
            data["filename"] = filename
        if model_name:
            data["model"] = model_name
        return self._post("/api/export/pdf", data)

    # Drawing API
    def create_drawing(
        self,
        model_name: Optional[str] = None,
        drawing_name: Optional[str] = None,
        template: Optional[str] = None,
        display: bool = True,
        activate: bool = True,
        show_dimensions: bool = True,
    ) -> Dict[str, Any]:
        data: Dict[str, Any] = {
            "display": display,
            "activate": activate,
            "show_dimensions": show_dimensions,
        }
        if model_name:
            data["model"] = model_name
        if drawing_name:
            data["drawing"] = drawing_name
        if template:
            data["template"] = template
        return self._post("/api/drawing/create", data)

    def show_drawing_dimensions(
        self,
        drawing_name: Optional[str] = None,
        model_name: Optional[str] = None,
    ) -> Dict[str, Any]:
        data: Dict[str, Any] = {}
        if drawing_name:
            data["drawing"] = drawing_name
        if model_name:
            data["model"] = model_name
        return self._post("/api/drawing/show_dimensions", data)


