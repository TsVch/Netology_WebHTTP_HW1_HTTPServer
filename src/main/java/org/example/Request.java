
package org.example;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class Request {

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final InputStream body;
    private final Map<String, List<String>> queryParams;

    public Request(
            String method,
            String path,
            Map<String, String> headers,
            InputStream body
    ) {
        this.method = method;

        URI uri = URI.create(path);
        String rawPath = uri.getRawPath();

        this.path = (rawPath == null || rawPath.isEmpty())
                ? "/"
                : rawPath;

        this.queryParams = parseQueryParams(uri);

        this.headers = Collections.unmodifiableMap(
                new LinkedHashMap<>(headers)
        );

        this.body = body;
    }

    private Map<String, List<String>> parseQueryParams(URI uri) {
        List<NameValuePair> pairs = URLEncodedUtils.parse(
                uri,
                StandardCharsets.UTF_8
        );

        Map<String, List<String>> params = new LinkedHashMap<>();

        for (NameValuePair pair : pairs) {
            params.computeIfAbsent(
                    pair.getName(),
                    key -> new ArrayList<>()
            ).add(pair.getValue());
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

    /**
     * Возвращает первое значение параметра Query.
     * Если параметр отсутствует, возвращает null.
     */
    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);

        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    /**
     * Возвращает все параметры Query,
     * включая параметры с повторяющимися именами.
     */
    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }
}