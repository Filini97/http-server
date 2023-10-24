import java.io.*;
import java.util.*;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final Map<String, List<String>> queryParams;
    private final static String GET = "GET";
    private final static String POST = "POST";

    private Request(String method, String path, List<String> headers,
                    Map<String, List<String>> queryParams, Map<String, List<String>> postParams, InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
    }

    public static Request getRequest(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        final var methods = List.of(GET, POST);
        final var limit = 4096;
        final var in = new BufferedInputStream(inputStream);

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(outputStream);
            return null;
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(outputStream);
            return null;
        }

        final var method = requestLine[0];
        if (!methods.contains(method)) {
            badRequest(outputStream);
            return null;
        }

        final var pathWithQuery = requestLine[1];
        if (!pathWithQuery.startsWith("/")) {
            badRequest(outputStream);
            return null;
        }
        final String path;
        final Map<String, List<String>> query;

        if (pathWithQuery.contains("?")) {
            String[] value = pathWithQuery.split("\\?");
            path = value[0];
            String queryLine = value[1];
            query = parseToQuery(queryLine);
        } else {
            path = pathWithQuery;
            query = null;
        }

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(outputStream);
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        Map<String, List<String>> post = null;

        // для GET тела нет
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);

            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                final var body = new String(bodyBytes);
                System.out.println(body);

                if (body.contains("=")) {
                    post = parseToQuery(body);
                }
            }
        }
        return new Request(method, path, headers, query, post, in);
    }

    private static Map<String, List<String>> parseToQuery(String queryLine) {
        //HashMap<String, List<String>> map = new HashMap<>();

    }

    public List<String> getQueryParam(String name) {
        return queryParams.get(name);
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


}
