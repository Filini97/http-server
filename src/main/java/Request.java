import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private static Map<String, String> queryParams;
    // private final List<NameValuePair> queryParams;
    private static final String GET = "GET";
    private static final String POST = "POST";

    public Request(String method, String path, List<String> headers, Map<String, String> queryParams) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
    }

     public static Request getRequest(InputStream inputStream, BufferedOutputStream outputStream) throws IOException, URISyntaxException {
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
         System.out.println("Метод - " + method);

        URI uri = URI.create(requestLine[1]);

        final var path = uri.getPath();
        if (!path.startsWith("/")) {
            badRequest(outputStream);
            return null;
        }
         System.out.println("Путь - " + path);

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

         List<NameValuePair> queryParList = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
         for (int i = 0; i < queryParList.size(); i++) {
             NameValuePair param = queryParList.get(i);
             queryParams.put(param.getName(), param.getValue());
         }
        return new Request(method, path, headers, queryParams);
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String paramName) {
        return queryParams.get(paramName);
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
