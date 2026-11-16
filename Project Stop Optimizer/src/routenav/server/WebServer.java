package routenav.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

/**
 * The whole web layer: the JDK's built-in HTTP server on one port, with the API
 * under /api/ and the page served from the static folder for everything else.
 */
public final class WebServer {

    private static final int THREADS = 8;

    private final HttpServer server;
    private final ExecutorService executor;

    public WebServer(int port, Path staticRoot) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(THREADS);
        server.setExecutor(executor);

        // The longest matching context wins, so /api/ takes precedence over /.
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new StaticFileHandler(staticRoot));
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }
}
