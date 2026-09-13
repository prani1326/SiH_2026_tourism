import json
import os
import sys

# Ensure project root is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from main import app

def generate_postman_collection():
    """
    Generates a production-ready Postman Collection v2.1.0 from FastAPI's OpenAPI specification.
    Includes folders categorized by feature tag, environment variables, Bearer auth headers,
    request body samples, and automated response status tests.
    """
    openapi = app.openapi()
    
    collection = {
        "info": {
            "_postman_id": "sih-tourist-app-v1-firestore",
            "name": f"{openapi.get('info', {}).get('title', 'Tourist App')} API - Firestore Production Suite",
            "description": openapi.get('info', {}).get('description', 'Comprehensive API collection for Tourist App with Firebase Firestore backend.'),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "variable": [
            {
                "key": "base_url",
                "value": "http://127.0.0.1:8000",
                "type": "string"
            },
            {
                "key": "auth_token",
                "value": "YOUR_FIREBASE_OR_JWT_TOKEN",
                "type": "string"
            }
        ],
        "item": []
    }

    # Group endpoints by their first tag
    folders = {}

    paths = openapi.get("paths", {})
    components = openapi.get("components", {}).get("schemas", {})

    def resolve_schema_sample(schema):
        if not schema:
            return {}
        if "$ref" in schema:
            schema_name = schema["$ref"].split("/")[-1]
            return resolve_schema_sample(components.get(schema_name, {}))
        
        s_type = schema.get("type", "object")
        if s_type == "string":
            if "example" in schema:
                return schema["example"]
            if schema.get("format") == "date-time":
                return "2026-09-04T12:00:00Z"
            if schema.get("format") == "date":
                return "2026-09-04"
            return "string"
        elif s_type == "integer":
            return schema.get("example", 1)
        elif s_type == "number":
            return schema.get("example", 100.0)
        elif s_type == "boolean":
            return schema.get("example", True)
        elif s_type == "array":
            items = schema.get("items", {})
            return [resolve_schema_sample(items)]
        elif s_type == "object":
            obj = {}
            props = schema.get("properties", {})
            for prop_name, prop_spec in props.items():
                obj[prop_name] = resolve_schema_sample(prop_spec)
            return obj
        return {}

    for path, methods in paths.items():
        for method, spec in methods.items():
            method_upper = method.upper()
            tags = spec.get("tags", ["General"])
            folder_name = tags[0] if tags else "General"

            if folder_name not in folders:
                folders[folder_name] = {
                    "name": folder_name,
                    "item": []
                }

            # URL formatting
            path_segments = [seg for seg in path.strip("/").split("/") if seg]
            url_raw = "{{base_url}}" + path
            
            # Extract query parameters
            query_params = []
            for param in spec.get("parameters", []):
                if param.get("in") == "query":
                    p_name = param.get("name")
                    p_schema = param.get("schema", {})
                    p_default = p_schema.get("default", "")
                    query_params.append({
                        "key": p_name,
                        "value": str(p_default),
                        "description": param.get("description", "")
                    })

            # Headers
            headers = [
                {
                    "key": "Content-Type",
                    "value": "application/json"
                }
            ]

            # Check if security or requires auth
            if spec.get("security") or any("auth" in t.lower() or "users" in t.lower() or "trips" in t.lower() or "bookings" in t.lower() for t in tags):
                headers.append({
                    "key": "Authorization",
                    "value": "Bearer {{auth_token}}"
                })

            # Body
            req_body = None
            if "requestBody" in spec:
                content = spec["requestBody"].get("content", {})
                json_content = content.get("application/json", {})
                if json_content and "schema" in json_content:
                    sample_dict = resolve_schema_sample(json_content["schema"])
                    req_body = {
                        "mode": "raw",
                        "raw": json.dumps(sample_dict, indent=2),
                        "options": {
                            "raw": {
                                "language": "json"
                            }
                        }
                    }

            summary = spec.get("summary") or spec.get("operationId") or f"{method_upper} {path}"

            item = {
                "name": summary,
                "request": {
                    "method": method_upper,
                    "header": headers,
                    "url": {
                        "raw": url_raw,
                        "host": ["{{base_url}}"],
                        "path": path_segments,
                        "query": query_params
                    },
                    "description": spec.get("description", "")
                },
                "response": [],
                "event": [
                    {
                        "listen": "test",
                        "script": {
                            "type": "text/javascript",
                            "exec": [
                                "pm.test(\"Status code is successful (2xx)\", function () {",
                                "    pm.expect(pm.response.code).to.be.oneOf([200, 201, 202, 204]);",
                                "});",
                                "pm.test(\"Response time is within acceptable SLA (< 2000ms)\", function () {",
                                "    pm.expect(pm.response.responseTime).to.be.below(2000);",
                                "});"
                            ]
                        }
                    }
                ]
            }

            if req_body:
                item["request"]["body"] = req_body

            # If login/register endpoint, auto-save auth_token
            if "/auth/login" in path or "/auth/register" in path:
                item["event"].append({
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": [
                            "if (pm.response.code === 200 || pm.response.code === 201) {",
                            "    var jsonData = pm.response.json();",
                            "    if (jsonData.access_token) {",
                            "        pm.collectionVariables.set(\"auth_token\", jsonData.access_token);",
                            "        console.log(\"Saved auth_token to collection variables\");",
                            "    }",
                            "}"
                        ]
                    }
                })

            folders[folder_name]["item"].append(item)

    # Sort folders alphabetically or by index
    sorted_folder_keys = sorted(folders.keys())
    for key in sorted_folder_keys:
        collection["item"].append(folders[key])

    output_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs", "Tourist_App_v1_Postman_Collection.json"))
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(collection, f, indent=2)

    print(f"Generated Postman Collection with {len(collection['item'])} folders and {sum(len(f['item']) for f in collection['item'])} API requests at:\n{output_path}")

if __name__ == "__main__":
    generate_postman_collection()
