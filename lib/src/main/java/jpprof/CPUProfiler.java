package jpprof;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import one.jfr.JfrReader;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

public class CPUProfiler {

    public static void Start(Duration duration, OutputStream out) throws IOException, InterruptedException {
        var jfrFile = File.createTempFile("jpprof", "jfr");
        AsyncProfiler.getInstance().execute(buildStartCommand(jfrFile.getAbsolutePath()));
        Thread.sleep(duration);
        AsyncProfiler.getInstance().stop();
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
