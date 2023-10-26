import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Request {
    private final String method;
    private static String path;
    private final List<String> headers;
    private static List<NameValuePair> queryParams;
    private final static String GET = "GET";
    private final static String POST = "POST";

    private Request(String method, String path, List<String> headers, List<NameValuePair> queryParams) {
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
        System.out.println(method);

        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            badRequest(outputStream);
            return null;
        }
        System.out.println(path);

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
            }
        }

        outputStream.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        outputStream.flush();

        String queryString = extractQueryString(requestLine[1]);
        List<NameValuePair> queryParams = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);

        return new Request(method, path, headers, queryParams);
    }

    public void setQueryParams() {
        String queryString = extractQueryString(path);
        queryParams = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
    }
    public static List<NameValuePair> getQueryParams() {
        return queryParams;
    }
    public String getQueryParam(String paramName) {
        for (NameValuePair param : queryParams) {
            if (param.getName().equals(paramName)) {
                return param.getValue();
            }
        }
        return null;
    }

    // извлечение строки запроса
    private static String extractQueryString(String requestLine) {
        int questionMarkIndex = requestLine.indexOf("?");
        if (questionMarkIndex >= 0) {
            return requestLine.substring(questionMarkIndex + 1);
        }
        return "";
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

    // from Google guava with modifications
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
