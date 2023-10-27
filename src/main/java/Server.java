import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    int port;
    ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(int port, int threads) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(threads);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> {
                    connect(socket);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

    public void connect(Socket socket) {
        try (socket;
             final var in = socket.getInputStream();
             final var out = new BufferedOutputStream(socket.getOutputStream());) {

            Request request = Request.getRequest(in, out);
            var handlerPath = handlers.get(request.getMethod());


            if (handlerPath == null) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            var handler = handlerPath.get(request.getPath());
            if (handler == null) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }
            handler.handle(request, out);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}