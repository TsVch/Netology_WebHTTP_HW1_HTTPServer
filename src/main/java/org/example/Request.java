
package org.example;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.RequestContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final InputStream body;

    private List<Part> parts;

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

    public Part getPart(String name) {
        return getParts().stream()
                .filter(part -> part.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<Part> getParts() {
        if (parts != null) {
            return parts;
        }

        String contentType = getHeader("Content-Type");

        if (contentType == null
                || !contentType.toLowerCase()
                .startsWith("multipart/form-data")) {
            return Collections.emptyList();
        }

        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            FileUpload upload = new FileUpload(factory);

            RequestContext context = new RequestContext() {
                @Override
                public String getCharacterEncoding() {
                    return StandardCharsets.UTF_8.name();
                }

                @Override
                public String getContentType() {
                    return contentType;
                }

                @Override
                public int getContentLength() {
                    String length = getHeader("Content-Length");

                    if (length == null) {
                        return -1;
                    }

                    try {
                        return Integer.parseInt(length);
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return body;
                }
            };

            List<FileItem> items = upload.parseRequest(context);
            List<Part> result = new ArrayList<>();

            for (FileItem item : items) {
                result.add(new Part(
                        item.getFieldName(),
                        item.isFormField() ? null : item.getName(),
                        item.getContentType(),
                        item.get()
                ));
            }

            parts = Collections.unmodifiableList(result);
            return parts;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse multipart/form-data request", e
            );
        }
    }

    private String getHeader(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}