import java.io.BufferedOutputStream;

public class Main {
    private static final int SERVER_SOCKET = 9999;
    private static final int POOLS = 64;
    public class Main {
        public static void main(String[] args){
            final var server = new Server();
            // код инициализации сервера (из вашего предыдущего ДЗ)

            // добавление хендлеров (обработчиков)
            server.addHandler("GET", "/messages", new Handler() {
                public void handle(Request request, BufferedOutputStream responseStream) {
                    // TODO: handlers code
                }
            });
            server.addHandler("POST", "/messages", new Handler() {
                public void handle(Request request, BufferedOutputStream responseStream) {
                    // TODO: handlers code
                }
            });

            server.start(SERVER_SOCKET);
        }
    }