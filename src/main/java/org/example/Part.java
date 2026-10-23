package org.example;

public class Part {
    private final String name;
    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public Part(
            String name,
            String fileName,
            String contentType,
            byte[] content
    ) {
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content.clone();
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public boolean isFile() {
        return fileName != null && !fileName.isEmpty();
    }

    public String getValue() {
        return new String(content, java.nio.charset.StandardCharsets.UTF_8);
    }
}