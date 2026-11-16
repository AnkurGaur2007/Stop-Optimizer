package routenav;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import routenav.server.WebServer;

/**
 * Starts the app with no framework: a JDK HTTP server, the routing engine and
 * the static folder next to this project.
 *
 * Usage: java -cp out routenav.Main [port]
 * The port defaults to 8080.
 */
public final class Main {

    private static final int DEFAULT_PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) {
        int port = readPort(args);
        if (port < 1) {
            return;
        }

        Path staticRoot = Path.of("static");
        if (!Files.isDirectory(staticRoot)) {
            System.err.println("Cannot find the static folder at " + staticRoot.toAbsolutePath());
            System.err.println("Start the app from the project folder, for example with run.bat or ./run.sh.");
            System.exit(1);
        }

        WebServer server;
        try {
            server = new WebServer(port, staticRoot);
        } catch (BindException e) {
            System.err.println("Port " + port + " is already in use. Close the other program or pick another port.");
            System.err.println("For example: java -cp out routenav.Main 8081");
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("The server could not start: " + e.getMessage());
            System.exit(1);
            return;
        }

        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            stopped.countDown();
        }));

        server.start();
        System.out.println("Smart City Route Navigator is running at http://localhost:" + port + "/");
        System.out.println("Press Ctrl+C to stop the server.");

        try {
            stopped.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int readPort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        try {
            int port = Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("The port must be a number between 1 and 65535, but got: " + args[0]);
            System.exit(2);
            return -1;
        }
    }
}
