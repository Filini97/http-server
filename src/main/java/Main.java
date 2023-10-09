
public class Main {
    public static final int PORT = 8090;
    public static final int Threads = 64;

    public static void main(String[] args) {
        Server server = new Server(PORT, Threads);
        server.start();
    }
}