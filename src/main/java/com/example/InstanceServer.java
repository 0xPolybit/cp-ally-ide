package com.example;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Manages single-instance coordination and cpally:// URL forwarding via a
 * loopback TCP socket. The primary instance binds the port and listens; any
 * subsequent instance connects, writes its URL, and exits.
 */
final class InstanceServer {

    private static final int PORT = 19742;
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;

    private ServerSocket serverSocket;

    /**
     * Tries to bind the loopback socket. Returns {@code true} if this process
     * is the primary instance (binding succeeded), {@code false} if another
     * instance is already running (BindException), or if binding fails for any
     * other reason (treated as secondary to be safe).
     */
    boolean tryBind() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT));
            return true;
        } catch (BindException e) {
            return false;
        } catch (IOException e) {
            DiagnosticLogger.warn("[InstanceServer] Could not bind port " + PORT + ": " + e.getMessage());
            return false;
        } catch (Error e) {
            // Catches InternalError from a broken/incomplete bundled JRE (missing java.security).
            // Degrade gracefully: single-instance IPC is disabled but the app still starts.
            DiagnosticLogger.warn("[InstanceServer] Networking unavailable, IPC disabled: " + e.getMessage());
            return true;
        }
    }

    /**
     * Starts the IPC listener daemon thread. Call this after the primary
     * instance has successfully called {@link #tryBind()}. Each incoming
     * connection is expected to send exactly one UTF-8 line containing a
     * cpally:// URL. The {@code handler} is always invoked on the EDT.
     */
    void startListening(Consumer<String> handler) {
        if (serverSocket == null || serverSocket.isClosed()) return;
        Thread listener = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(READ_TIMEOUT_MS);
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
                        String line = reader.readLine();
                        if (line != null && !line.isBlank()) {
                            String url = line.strip();
                            SwingUtilities.invokeLater(() -> handler.accept(url));
                        }
                    } catch (IOException ignored) {
                        // per-connection read errors are non-fatal
                    } finally {
                        try { client.close(); } catch (IOException ignored) {}
                    }
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        DiagnosticLogger.warn("[InstanceServer] Accept error: " + e.getMessage());
                    }
                }
            }
        }, "cpally-ipc-listener");
        listener.setDaemon(true);
        listener.start();
    }

    /**
     * Connects to the primary instance and forwards the given URL. Returns
     * {@code true} if the URL was sent successfully, {@code false} on any
     * connection or write failure (e.g., primary just exited).
     */
    boolean tryForward(String url) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), CONNECT_TIMEOUT_MS);
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(url);
                writer.newLine();
                writer.flush();
            }
            return true;
        } catch (IOException | Error e) {
            DiagnosticLogger.warn("[InstanceServer] Forward failed: " + e.getMessage());
            return false;
        }
    }

    /** Closes the server socket, stopping the listener thread. */
    void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }
}
