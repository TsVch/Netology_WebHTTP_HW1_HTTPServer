package org.example;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final InputStream body;

    public Request(
            String method,
            String path,
            Map<String, String> headers,
            InputStream body
    ) {
        this.method = method;
        this.path = path;
        this.headers = Collections.unmodifiableMap(
                new LinkedHashMap<>(headers)
        );
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }
}