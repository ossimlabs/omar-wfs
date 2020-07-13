let json = {
    "swagger": "2.0",
    "paths": {
        "/wfs/describeFeatureType": {
            "get": {
                "summary": "Describe the feature from the server",
                "operationId": "describeFeatureType",
                "produces": [
                    "application/xml"
                ],
                "parameters": [
                    {
                        "name": "service",
                        "in": "query",
                        "description": "OGC Service type",
                        "required": true,
                        "type": "string",
                        "default": "WFS",
                        "enum": [
                            "WFS"
                        ]
                    },
                    {
                        "name": "version",
                        "in": "query",
                        "description": "Version to request",
                        "required": true,
                        "type": "string",
                        "default": "1.1.0",
                        "enum": [
                            "1.1.0"
                        ]
                    },
                    {
                        "name": "request",
                        "in": "query",
                        "description": "Request type",
                        "required": true,
                        "type": "string",
                        "default": "DescribeFeatureType",
                        "enum": [
                            "DescribeFeatureType"
                        ]
                    },
                    {
                        "name": "typeName",
                        "in": "query",
                        "description": "Type Name",
                        "required": true,
                        "type": "string",
                        "default": "omar:raster_entry"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "successful operation",
                        "schema": {
                            "type": "object"
                        }
                    }
                }
            }
        },
        "/wfs/getCapabilities": {
            "get": {
                "summary": "Get the capabilities of the server",
                "operationId": "getCapabilities",
                "produces": [
                    "application/xml"
                ],
                "parameters": [
                    {
                        "name": "service",
                        "in": "query",
                        "description": "OGC Service type",
                        "required": true,
                        "type": "string",
                        "default": "WFS",
                        "enum": [
                            "WFS"
                        ]
                    },
                    {
                        "name": "version",
                        "in": "query",
                        "description": "Version to request",
                        "required": true,
                        "type": "string",
                        "default": "1.1.0",
                        "enum": [
                            "1.1.0"
                        ]
                    },
                    {
                        "name": "request",
                        "in": "query",
                        "description": "Request type",
                        "required": true,
                        "type": "string",
                        "default": "GetCapabilities",
                        "enum": [
                            "GetCapabilities"
                        ]
                    }
                ],
                "responses": {
                    "200": {
                        "description": "successful operation",
                        "schema": {
                            "type": "object"
                        }
                    }
                }
            }
        },
        "/wfs/getFeature": {
            "get": {
                "summary": "Get features from the server",
                "operationId": "getFeature",
                "produces": [
                    "application/xml",
                    "application/json"
                ],
                "parameters": [
                    {
                        "name": "service",
                        "in": "query",
                        "description": "OGC service type",
                        "required": true,
                        "type": "string",
                        "default": "WFS",
                        "enum": [
                            "WFS"
                        ]
                    },
                    {
                        "name": "version",
                        "in": "query",
                        "description": "Version to request",
                        "required": true,
                        "type": "string",
                        "default": "1.1.0",
                        "enum": [
                            "1.1.0"
                        ]
                    },
                    {
                        "name": "request",
                        "in": "query",
                        "description": "Request type",
                        "required": true,
                        "type": "string",
                        "default": "GetFeature",
                        "enum": [
                            "GetFeature"
                        ]
                    },
                    {
                        "name": "typeName",
                        "in": "query",
                        "description": "Type name",
                        "required": true,
                        "type": "string",
                        "default": "omar:raster_entry"
                    },
                    {
                        "name": "filter",
                        "in": "query",
                        "description": "Filter",
                        "required": false,
                        "type": "string"
                    },
                    {
                        "name": "resultType",
                        "in": "query",
                        "description": "Result type",
                        "required": false,
                        "type": "string",
                        "enum": [
                            "results",
                            "hits"
                        ]
                    },
                    {
                        "name": "outputFormat",
                        "in": "query",
                        "description": "Output format",
                        "required": false,
                        "type": "string",
                        "enum": [
                            "JSON",
                            "KML",
                            "CSV",
                            "GML2",
                            "GML3",
                            "GML32",
                            "WMS111",
                            "WMS130"
                        ]
                    },
                    {
                        "name": "sortBy",
                        "in": "query",
                        "description": "Sort by",
                        "required": false,
                        "type": "string"
                    },
                    {
                        "name": "propertyName",
                        "in": "query",
                        "description": "Property name (comma separated fields)",
                        "required": false,
                        "type": "string"
                    },
                    {
                        "name": "maxFeatures",
                        "in": "query",
                        "description": "Maximum Features in the result",
                        "required": false,
                        "type": "integer",
                        "default": 10
                    },
                    {
                        "name": "startIndex",
                        "in": "query",
                        "description": "Starting offset",
                        "required": false,
                        "type": "integer"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "successful operation",
                        "schema": {
                            "type": "object"
                        }
                    }
                }
            }
        }
    }
}
let paths = Object.keys(json.paths);
let methods, innerJson, name, parameters, request;

describe('Automated tests for the omar-wfs methods', () => {
    paths.forEach((path) => {
        methods = Object.keys(json.paths[path]);
        methods.forEach((method) => {
            innerJson = json.paths[path][method];
            name = innerJson["operationId"];
            parameters = innerJson["parameters"]
            request = "?"
            parameters.forEach((parameter) => {
                if(parameter["default"])
                    request = request + parameter["name"] + "=" + parameter["default"] + "&";
                if(parameter["enum"])
                    request = request + parameter["name"] + "=" + parameter["enum"][0] + "&";
            })
            request = request.substring(0, request.length - 1);
            it(`Should test 200 code for ${name} default values`, () => {
                cy.request(method, path + request)
                    .then((response) => {
                        expect(response.status).to.eq(200)
                    })
            })
            it(`Should test response header for ${name}`, () => {
                 cy.request(method, path + request)
                     .then((response) => {
                         expect(response).to.have.property("headers")
                     })
            })
        })
    })
})