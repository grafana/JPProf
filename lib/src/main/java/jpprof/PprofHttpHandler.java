package jpprof;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Map;

import com.google.common.base.Splitter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PprofHttpHandler implements HttpHandler {

    public PprofHttpHandler() {
    }

    @Override
    public void handle(HttpExchange t) throws IOException {

        String path = t.getRequestURI().getPath();
        // we only support /debug/pprof/profile for now.
        if (!path.equals("/debug/pprof/profile")) {
            t.sendResponseHeaders(404, 0);
            t.close();
            return;
        }
        final Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator('=')
                .split(t.getRequestURI().getRawQuery());
        var duration = Duration.ofSeconds(Integer.parseInt(map.get("seconds")));
        try {
            t.getRequestBody().close();
            t.getResponseHeaders().set("Content-Encoding", "gzip");
            t.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            CPUProfiler.Start(duration, t.getResponseBody());
        } catch (Exception e) {
            t.sendResponseHeaders(500, 0);
            return;
        } finally {
            t.close();

        }

    }
}
