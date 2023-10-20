import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private byte[] body;
    private final String path;
    private final List<String> headers;
    private final InputStream in;
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
        this.in = in;
    }

    public static Request getRequest(InputStream in, BufferedOutputStream out) {
        return null;
    }

    public static String getMethod() {
        return null;
    }

    public static String getPath() {
        return null;
    }

}
