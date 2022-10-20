package jpprof;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import one.jfr.JfrReader;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

/**
 * CPUProfiler is a CPU profiler.
 */
public class CPUProfiler {
    private static final File tmpDir;
    private static final String nativeLibPath;

    static {
        try {
            tmpDir = Files.createTempDirectory("jpprof-").toFile();

            InputStream res = CPUProfiler.class.getResourceAsStream("/natives/libasyncProfiler.so");
            if (res == null) {
                throw new Exception("native lib not found");
            }

            Path nativeLibTargetPath = new File(tmpDir, "libasyncProfiler.so").toPath();
            Files.copy(res, nativeLibTargetPath, StandardCopyOption.REPLACE_EXISTING);
            nativeLibPath = nativeLibTargetPath.toString();
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
        var jfrFile = File.createTempFile("profile-", "jfr", tmpDir);
        var instance = AsyncProfiler.getInstance(nativeLibPath);
        instance.execute(buildStartCommand(jfrFile.getAbsolutePath()));
        Thread.sleep(duration.toMillis());
        instance.stop();

        try (var jfrReader = new JfrReader(jfrFile.getAbsolutePath());
                var outgzip = new GZIPOutputStream(out);) {
            jfr2pprof.Convert(jfrReader, outgzip);
        }
        jfrFile.delete();
    }

    private static String buildStartCommand(String dst) {
        StringBuilder sb = new StringBuilder();
        sb.append("start,event=").append(Events.CPU);
        sb.append(",interval=").append(10);
        sb.append(",file=").append(dst).append(",jfr");
        return sb.toString();
    }

}
