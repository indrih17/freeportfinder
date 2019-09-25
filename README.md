# freeportfinder

A micro jvm library that does one thing and one thing only: finds a free local port (mainly) for testing purposes.

```groovy
compile 'indrih17.free-port-finder:free-port-finder:1.0.0'
```

Finding a free local port is as easy as:
```kotlin
val port = FreePortFinder.findFreeLocalPort()
```

Which can then be used to set up local Jetty without hitting
```
java.net.BindException: Address already in use
```

## Contributors
[Shlomi Alfasi](https://github.com/shlomialfasi)

[Indrih17](https://github.com/indrih17)