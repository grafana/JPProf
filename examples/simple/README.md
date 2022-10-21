# Demo Java app

Run with:

```bash
./gradlew run
```

Get a CPU profile with:

```bash
go tool pprof -http :6060 "http://localhost:4001/debug/pprof/profile?seconds=10"
```
