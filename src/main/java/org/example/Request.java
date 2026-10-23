package org.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private static final String FORM_CONTENT_TYPE =
            "application/x-www-form-urlencoded";

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final InputStream body;
    private final Map<String, List<String>> postParams;

    public Request(
            String method,
            String path,
            Map<String, String> headers,
            InputStream body
    ) throws IOException {
        this.method = method;
        this.path = path;

        this.headers = Collections.unmodifiableMap(
                new LinkedHashMap<>(headers)
        );

        byte[] bodyBytes = body.readAllBytes();

        // Сохраняем возможность повторно прочитать тело через getBody().
        this.body = new ByteArrayInputStream(bodyBytes);

        String contentType = this.headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("Content-Type"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");

        if (contentType.split(";", 2)[0].trim()
                .equalsIgnoreCase(FORM_CONTENT_TYPE)) {
            this.postParams = parseForm(
                    new String(bodyBytes, StandardCharsets.UTF_8)
            );
        } else {
            this.postParams = Collections.emptyMap();
        }
    }

    private Map<String, List<String>> parseForm(String formBody) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        if (formBody.isEmpty()) {
            return Collections.unmodifiableMap(params);
        }

        String[] pairs = formBody.split("&", -1);

        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }

            int separatorIndex = pair.indexOf('=');

            String rawName;
            String rawValue;

            if (separatorIndex >= 0) {
                rawName = pair.substring(0, separatorIndex);
                rawValue = pair.substring(separatorIndex + 1);
            } else {
                rawName = pair;
                rawValue = "";
            }

            String name = URLDecoder.decode(
                    rawName, StandardCharsets.UTF_8
            );
            String value = URLDecoder.decode(
                    rawValue, StandardCharsets.UTF_8
            );

            params.computeIfAbsent(name, key -> new ArrayList<>())
                    .add(value);
        }

        Map<String, List<String>> immutableParams = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            immutableParams.put(
                    entry.getKey(),
                    Collections.unmodifiableList(
                            new ArrayList<>(entry.getValue())
                    )
            );
        }

        return Collections.unmodifiableMap(immutableParams);
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

    public String getPostParam(String name) {
        List<String> values = postParams.get(name);

        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    public Map<String, List<String>> getPostParams() {
        return postParams;
    }
}