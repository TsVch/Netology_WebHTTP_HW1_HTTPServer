package org.example;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int THREAD_COUNT = 64;
    private static final int MAX_HEADER_SIZE = 64 * 1024;
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    private final ExecutorService threadPool =
            Executors.newFixedThreadPool(THREAD_COUNT);

    private final Map<String, Handler> handlers =
            new ConcurrentHashMap<>();

    public void addHandler(String method, String path, Handler handler) {
        if (method == null || path == null || handler == null) {
            throw new IllegalArgumentException(
                    "Метод, путь и обработчик не должны быть null"
            );
        }

        handlers.put(createKey(method, path), handler);
    }

    private String createKey(String method, String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path;
    }

    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(
                    "Сервер запущен: http://localhost:" + port
            );

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    public void start() {
        listen(9999);
    }

    public void handleConnection(Socket socket) {
        try (
                socket;
                InputStream input = socket.getInputStream();
                BufferedOutputStream output =
                        new BufferedOutputStream(socket.getOutputStream())
        ) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] delimiter = {'\r', '\n', '\r', '\n'};
            int matched = 0;
            int current;

            // Читаем заголовки до CRLF CRLF.
            while ((current = input.read()) != -1) {
                buffer.write(current);

                if (buffer.size() > MAX_HEADER_SIZE) {
                    sendError(
                            output,
                            431,
                            "Request Header Fields Too Large"
                    );
                    return;
                }

                if (current == delimiter[matched]) {
                    matched++;

                    if (matched == delimiter.length) {
                        break;
                    }
                } else {
                    matched = current == delimiter[0] ? 1 : 0;
                }
            }

            if (buffer.size() == 0) {
                return;
            }

            if (matched != delimiter.length) {
                sendError(output, 400, "Bad Request");
                return;
            }

            byte[] headerBytes = buffer.toByteArray();
            int headerLength = headerBytes.length - delimiter.length;

            String headerText = new String(
                    headerBytes,
                    0,
                    headerLength,
                    StandardCharsets.ISO_8859_1
            );

            String[] lines = headerText.split("\r\n");
            String[] requestLine = lines[0].split(" ", 3);

            if (requestLine.length != 3
                    || requestLine[0].isEmpty()
                    || requestLine[1].isEmpty()
                    || !requestLine[2].startsWith("HTTP/")) {
                sendError(output, 400, "Bad Request");
                return;
            }

            String method = requestLine[0].toUpperCase(Locale.ROOT);

            // Важно: сохраняем исходный request-target с Query.
            String requestTarget = requestLine[1];

            Map<String, String> headers = new ConcurrentHashMap<>();

            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');

                if (colon > 0) {
                    String name = lines[i]
                            .substring(0, colon)
                            .trim()
                            .toLowerCase(Locale.ROOT);

                    String value = lines[i]
                            .substring(colon + 1)
                            .trim();

                    headers.put(name, value);
                }
            }

            int contentLength = 0;

            if (headers.containsKey("content-length")) {
                try {
                    contentLength = Integer.parseInt(
                            headers.get("content-length")
                    );
                } catch (NumberFormatException e) {
                    sendError(output, 400, "Invalid Content-Length");
                    return;
                }
            }

            if (contentLength < 0 || contentLength > MAX_BODY_SIZE) {
                sendError(output, 413, "Payload Too Large");
                return;
            }

            byte[] body = input.readNBytes(contentLength);

            if (body.length != contentLength) {
                sendError(output, 400, "Incomplete Request Body");
                return;
            }

            // Request самостоятельно разделяет путь и Query.
            Request request = new Request(
                    method,
                    requestTarget,
                    headers,
                    new ByteArrayInputStream(body)
            );

            // Ищем обработчик по нормализованному пути без Query.
            Handler handler = handlers.get(
                    createKey(request.getMethod(), request.getPath())
            );

            if (handler == null) {
                sendError(output, 404, "Not Found");
                return;
            }

            handler.handle(request, output);
            output.flush();

        } catch (IOException e) {
            System.err.println(
                    "Ошибка обработки подключения: " + e.getMessage()
            );
        } catch (IllegalArgumentException e) {
            System.err.println(
                    "Некорректный HTTP-запрос: " + e.getMessage()
            );

            // Соединение будет закрыто благодаря try-with-resources.
        }
    }

    private void sendError(
            BufferedOutputStream output,
            int status,
            String message
    ) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        String response = "HTTP/1.1 " + status + " " + message + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";

        output.write(response.getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }
}