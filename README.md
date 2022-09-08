# yaci-cli (WIP)

## Build from source

```
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
./gradlew clean build
```

## Build Native Image

- Install GraalVM and set the path

```
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
./gradlew clean build nativeCompile
```
