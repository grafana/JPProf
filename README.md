# JPProf

JPProf brings go [pprof][pprof] tooling to the Java world.

The library implements an [http handler](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpHandler.html)
that can serve runtime profiling data in the format expected by the [pprof][pprof] visualization tool.

## Usage

For now the library only support basic http handler so you can either plug the handler
in your server or start a new one as below:

```java
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import jpprof.PprofHttpHandler;

public class Main {
    public static void main(String[] args) throws Exception {
        var server = HttpServer.create(new InetSocketAddress(4001), 0);
        server.createContext("/", new PprofHttpHandler());
        server.start();
    }

}
```

Once the handler is setup you can directly use go tool to fetch CPU profile.

```bash
go tool pprof -http :6060 http://localhost:4001/debug/pprof/profile\?seconds\=10
```

Currently the library only supports CPU profiling.

[pprof]: https://go.dev/blog/pprof
