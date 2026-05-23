package com.example.ChessGame.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe Stockfish service using a pool of engine processes.
 *
 * Each engine instance is held in a blocking queue. A caller borrows one,
 * uses it, then returns it. If the engine dies mid-use it is discarded and
 * a fresh one is created on the next borrow.
 *
 * Configuration (application.properties):
 *   stockfish.path=stockfish          # binary name or absolute path
 *   stockfish.pool-size=4             # how many parallel engine processes
 *   stockfish.timeout-ms=2000         # max ms to wait for bestmove
 */
@Service
public class StockfishService {

    private static final Logger LOG = Logger.getLogger(StockfishService.class.getName());

    @Value("${stockfish.path:stockfish}")
    private String stockfishPath;

    @Value("${stockfish.pool-size:4}")
    private int poolSize;

    @Value("${stockfish.timeout-ms:2000}")
    private long timeoutMs;

    /** Pool of ready-to-use engine instances. */
    private BlockingQueue<EngineInstance> pool;

    // Lazy init: build pool on first use so Spring context loads even when
    // Stockfish is not installed (e.g. during unit tests without AI).
    private synchronized void ensurePool() {
        if (pool != null) return;
        pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            try {
                pool.add(createEngine());
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not start Stockfish instance " + i, e);
            }
        }
        if (pool.isEmpty()) {
            LOG.severe("No Stockfish instances started. AI moves will be unavailable.");
        }
    }

    /** Ask Stockfish for the best move given a FEN string. Returns null on failure. */
    public String getBestMoveFromFen(String fen, int movetimeMs) {
        ensurePool();

        EngineInstance engine = null;
        try {
            engine = pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (engine == null) {
                LOG.warning("All Stockfish instances busy; skipping AI move.");
                return null;
            }
            return engine.bestMove(fen, movetimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Stockfish I/O error; discarding instance.", e);
            closeQuietly(engine);
            engine = null;         // do not return broken instance
            tryReplenish();        // spawn a replacement asynchronously
            return null;
        } finally {
            if (engine != null) {
                // return healthy instance to pool
                if (!pool.offer(engine)) {
                    closeQuietly(engine); // pool was somehow full; just close it
                }
            }
        }
    }

    /** Spawn a replacement engine in the background after one dies. */
    private void tryReplenish() {
        Thread.ofVirtual().start(() -> {
            try {
                EngineInstance fresh = createEngine();
                if (!pool.offer(fresh)) closeQuietly(fresh);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not replenish Stockfish pool.", e);
            }
        });
    }

    private EngineInstance createEngine() throws IOException {
        EngineInstance e = new EngineInstance(stockfishPath);
        e.init();
        return e;
    }

    @PreDestroy
    public void shutdown() {
        if (pool == null) return;
        EngineInstance e;
        while ((e = pool.poll()) != null) {
            closeQuietly(e);
        }
    }

    private static void closeQuietly(EngineInstance e) {
        if (e != null) {
            try { e.close(); } catch (Exception ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: one Stockfish process with its own stdin/stdout streams.
    // Not thread-safe on its own — safety comes from the pool above.
    // -------------------------------------------------------------------------
    private static class EngineInstance implements Closeable {

        private final String path;
        private Process process;
        private BufferedWriter stdin;
        private BufferedReader stdout;

        EngineInstance(String path) {
            this.path = path;
        }

        void init() throws IOException {
            ProcessBuilder pb = new ProcessBuilder(path);
            pb.redirectErrorStream(true);
            process = pb.start();
            stdin  = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            send("uci");
            waitFor("uciok");
            send("isready");
            waitFor("readyok");
        }

        String bestMove(String fen, int movetimeMs) throws IOException {
            if (!isAlive()) throw new IOException("Stockfish process is not running");
            send("position fen " + fen);
            send("go movetime " + movetimeMs);

            String line;
            while ((line = stdout.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    return (parts.length >= 2 && !"(none)".equals(parts[1])) ? parts[1] : null;
                }
            }
            return null;
        }

        boolean isAlive() {
            return process != null && process.isAlive();
        }

        private void send(String cmd) throws IOException {
            stdin.write(cmd + "\n");
            stdin.flush();
        }

        private void waitFor(String token) throws IOException {
            String line;
            while ((line = stdout.readLine()) != null) {
                if (line.trim().toLowerCase(Locale.ROOT).contains(token)) return;
            }
        }

        @Override
        public void close() {
            try { send("quit"); } catch (Exception ignored) {}
            try { stdin.close();  } catch (Exception ignored) {}
            try { stdout.close(); } catch (Exception ignored) {}
            if (process != null)  process.destroyForcibly();
        }
    }
}