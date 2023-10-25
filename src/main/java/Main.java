import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    private static final int SERVER_SOCKET = 9999;
    private static final int POOLS = 64;

    public static void main(String[] args) {
        final var server = new Server(SERVER_SOCKET, POOLS);
        startHandlers(server);
        server.start(SERVER_SOCKET);
    }

        private static void startHandlers (Server server) {

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/links.html", (request, responseStream) -> {
                try {
                    final Path filePath = Path.of(".", "public", "/links.html");
                    final String mimeType = Files.probeContentType(filePath);
                    final long length = Files.size(filePath);
                    responseStream.write(("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
                    ).getBytes());
                    Files.copy(filePath, responseStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        });

        server.addHandler("GET", "/events.html", (request, responseStream) -> {
                try {
                    final Path filePath = Path.of(".", "public", "events.html");
                    final String mimeType = Files.probeContentType(filePath);
                    final String template = Files.readString(filePath);
                    final byte[] content = template.replace("time", LocalDateTime.now().toString()).getBytes();
                    responseStream.write(("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
                    ).getBytes());
                    responseStream.write(content);
                    responseStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        });

        server.addHandler("POST", "/", (request, responseStream) -> {
            try {
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}