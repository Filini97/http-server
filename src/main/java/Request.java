import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> postParams;
    private final static String GET = "GET";
    private final static String POST = "POST";

    private Request(String method, String path,  List<String> headers,
                    Map<String, List<String>> queryParams, Map<String, List<String>> postParams, InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.postParams = postParams;
    }

    public static Request getRequest(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        final var methods = List.of(GET, POST);
        final var limit = 4096;
        final var in = new BufferedReader(new InputStreamReader(inputStream));

        in.mark(limit);

        //читаем request line
        final var requestLineReader = in.readLine();
        final var parts = requestLineReader.split(" ");

        if (parts.length != 3) {
            badRequest(outputStream);
            return null;
        }

        final var method = parts[0];
        if (!methods.contains(method)) {
            badRequest(outputStream);
            return null;
        }

        final var queryPath = parts[1];
        if (!queryPath.startsWith("/")) {
            badRequest(outputStream);
            return null;
        }

        final var headers = ...



    }

    public static String getMethod() {
        return null;
    }

    public static String getPath() {
        return null;
    }




    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

}
