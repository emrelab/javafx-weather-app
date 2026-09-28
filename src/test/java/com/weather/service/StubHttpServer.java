package com.weather.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Testler için minimal HTTP/1.1 sunucusu.
 *
 * <p>Bilinçli olarak ham {@link ServerSocket} kullanır: JDK'nın {@code jdk.httpserver}
 * modülüne bağlanmak, üretim {@code module-info.java} dosyasına test amaçlı bir
 * {@code requires} eklemeyi gerektirirdi. Bu yaklaşım testleri modül grafiğinden
 * tamamen bağımsız tutar.
 */
final class StubHttpServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    StubHttpServer(int status, String body) throws IOException {
        this.serverSocket = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"));
        Thread acceptThread = new Thread(() -> serve(status, body), "stub-http-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Stub'ın kök adresi, örn. {@code http://127.0.0.1:52341}. */
    String baseUrl() {
        return "http://127.0.0.1:" + serverSocket.getLocalPort();
    }

    /** Son isteğin ham query string'i; hiç istek gelmediyse {@code null}. */
    String lastQuery() {
        return lastQuery.get();
    }

    int requestCount() {
        return requestCount.get();
    }

    private void serve(int status, String body) {
        while (!serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept()) {
                requestCount.incrementAndGet();
                recordRequestLine(socket);
                writeResponse(socket, status, body);
            } catch (IOException e) {
                return; // close() çağrıldı
            }
        }
    }

    private void recordRequestLine(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));

        String requestLine = reader.readLine();
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length > 1) {
                String target = parts[1];
                int queryStart = target.indexOf('?');
                lastQuery.set(queryStart >= 0 ? target.substring(queryStart + 1) : "");
            }
        }

        String header;
        while ((header = reader.readLine()) != null && !header.isEmpty()) {
            // Başlıkları tüket; GET isteğinde gövde yok.
        }
    }

    private void writeResponse(Socket socket, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);

        String head = "HTTP/1.1 " + status + " Stub\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + payload.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        OutputStream out = socket.getOutputStream();
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(payload);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}
