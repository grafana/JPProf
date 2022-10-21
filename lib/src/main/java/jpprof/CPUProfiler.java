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
            nativeLibPath = copyLibrary();
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

    /**
     * Copy the embedded native async-profiler library to
     * a tmp path, and returns the absolute path.
     */
    private static String copyLibrary() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        String embeddedLibPrefix = "/async-profiler-libs/libasyncProfiler";
        String embeddedLibSuffix = "";

        switch (osName) {
            case "linux":
                switch (osArch) {
                    case "amd64":
                        embeddedLibSuffix = "-linux-x64.so";
                        break;

                    case "aarch64":
                        embeddedLibSuffix = "-linux-arm64.so";
                        break;

                    default:
                        throw new Exception("Unsupported Linux arch: " + osArch);
                }
                break;

            case "mac os x":
                switch (osArch) {
                    case "x86_64":
                    case "aarch64":
                        embeddedLibSuffix = "-macos.so";
                        break;

                    default:
                        throw new Exception("Unsupported OSX arch: " + osArch);
                }
                break;

            default:
                throw new Exception("Unsupported OS: " + osName);
        }

        InputStream is = CPUProfiler.class.getResourceAsStream(embeddedLibPrefix + embeddedLibSuffix);
        if (is == null) {
            throw new Exception("native lib not found");
        }

        Path libCopyPath = new File(tmpDir, "libasyncProfiler.so").toPath().toAbsolutePath();
        Files.copy(is, libCopyPath, StandardCopyOption.REPLACE_EXISTING);
        return libCopyPath.toString();
    }

    private static String buildStartCommand(String dst) {
        StringBuilder sb = new StringBuilder();
        sb.append("start,event=").append(Events.CPU);
        sb.append(",interval=").append(10);
        sb.append(",file=").append(dst).append(",jfr");
        return sb.toString();
    }

}
