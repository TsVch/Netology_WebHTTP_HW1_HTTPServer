package org.example;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        Server server = new Server();

        server.addHandler("GET", "/", (request, response) -> {
            Path index = Path.of("public", "index.html");
            byte[] body = Files.readAllBytes(index);

            writeResponse(
                    response, 200, "OK",
                    "text/html; charset=UTF-8", body
            );
        });

        server.addHandler("GET", "/api/hello", (request, response) -> {
            byte[] body = """
                    {"message":"Hello from HTTP server!"}
                    """.trim().getBytes(StandardCharsets.UTF_8);

            writeResponse(
                    response, 200, "OK",
                    "application/json; charset=UTF-8", body
            );
        });

        server.addHandler("POST", "/messages", (request, response) -> {
            byte[] body = request.getBody().readAllBytes();

            writeResponse(
                    response, 200, "OK",
                    "text/plain; charset=UTF-8",
                    ("Получено сообщение: "
                            + new String(body, StandardCharsets.UTF_8))
                            .getBytes(StandardCharsets.UTF_8)
            );
        });

        server.addHandler("GET", "/messages", (request, response) -> {
            // Получаем первый параметр last из query-строки.
            String last = request.getQueryParam("last");

            String message;
            if (last != null) {
                message = "Список сообщений. Количество: " + last;
            } else {
                message = "Список сообщений";
            }

            byte[] body = message.getBytes(StandardCharsets.UTF_8);

            writeResponse(
                    response, 200, "OK",
                    "text/plain; charset=UTF-8", body
            );
        });

        server.listen(9999);
    }

    private static void writeResponse(
            BufferedOutputStream response,
            int status,
            String reason,
            String contentType,
            byte[] body
    ) throws IOException {
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";

        response.write(headers.getBytes(StandardCharsets.UTF_8));
        response.write(body);
        response.flush();
    }
}