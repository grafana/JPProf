package jpprof;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import one.jfr.JfrReader;
import one.profiler.AsyncProfiler;
import one.profiler.AsyncProfilerLoader;
import one.profiler.Events;

/**
 * CPUProfiler is a CPU profiler.
 */
public class CPUProfiler {
    private static final File tmpDir;

    static {
        try {
            tmpDir = Files.createTempDirectory("jpprof-").toFile();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start a CPU profile.
     *
     * @param duration the duration of the profile
     * @param out      the output stream
     * @throws IOException if an I/O error occurs
     */
    public static void start(Duration duration, OutputStream out) throws IOException, InterruptedException {
        File jfrFile = File.createTempFile("profile-", "jfr", tmpDir);
        AsyncProfiler instance = AsyncProfilerLoader.load();
        instance.execute(buildStartCommand(jfrFile.getAbsolutePath()));
        Thread.sleep(duration.toMillis());
        instance.stop();

        try (JfrReader jfrReader = new JfrReader(jfrFile.getAbsolutePath());
                OutputStream outgzip = new GZIPOutputStream(out);) {
            jfr2pprof.Convert(jfrReader, outgzip);
        }
        jfrFile.delete();
    }

    public static String buildStartCommand(String dst) {
        StringBuilder sb = new StringBuilder();
        sb.append("start,event=").append(Events.CPU);
        sb.append(",interval=").append(10_000_000);
        sb.append(",file=").append(dst).append(",jfr");
        return sb.toString();
    }

}
