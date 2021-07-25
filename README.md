# Kotlin RPC

[![CodeFactor](https://www.codefactor.io/repository/github/darvld/krpc/badge/main)](https://www.codefactor.io/repository/github/darvld/krpc/overview/main)
[![CI](https://github.com/darvld/krpc/actions/workflows/ci.yml/badge.svg)](https://github.com/darvld/krpc/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.darvld.krpc/krpc-runtime.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.darvld.krpc%22%20AND%20a:%22krpc-runtime%22)

A Kotlin Multiplatform library to generate and consume gRPC services directly from Kotlin. Check out
the [Sample project](https://github.com/darvld/krpc/tree/main/sample) to see a full list of available features, or see
the [Quick Start](docs/Basic.md) guide on how to add kRPC to your project.

> **Note:** At the moment, only Kotlin/JVM is supported as a target platform for code generation.

## Overview

Instead of using Protoc and .proto files to define services, kRPC uses Kotlin's interfaces and annotations. Any
interface annotated with `@Service` will be recognized as a definition by the kRPC compiler:

```kotlin
@Service
interface GpsService {
    @UnaryCall
    suspend fun locationForVehicle(vehicle: Vehicle): Location
}
```

The methods inside the service definitions should have the proper annotation to reflect the type of RPC call:

- `@UnaryCall` defines a single-request, single-response rpc method, suspending until the server replies.
- `@ServerStream` takes a single request and returns a response `Flow`, which you can later use to consume the responses
  streamed by the server. Server-stream methods don't suspend, instead the returned flow collector will suspend until
  the first value is received.
- `@ClientStream` takes a request flow and returns a single response, suspending until it is received.
- `@BidiStream` takes a request flow and returns response flow, thus enabling bidirectional streaming. Like
  with `@ServerStream`, this method does not suspend.

Method parameters and return types must be serializable.

The compiler will then generate two implementations of this interface: an abstract service provider, and a client.
Additionally, a helper class is generated: the service's Descriptor, which contains all the generated method
descriptors (on JVM) and can be used to create your own custom client/server implementations.

## Implementation

The kRPC compiler uses Google's [KSP](https://github.com/google/ksp) to process the annotations and then generates code
with [KotlinPoet](). On JVM, the runtime and generated stubs are implemented on top of
the [grpc-kotlin-stub](https://github.com/grpc/grpc-kotlin)
runtime.

## Resources

- [Getting Started](docs/Basic.md)
- [Service Definitions](docs/Advanced.md)
- [Roadmap](docs/Roadmap.md)

If you find the project useful, have an idea on how to improve it or if you find a bug, consider making a contribution
or opening a new issue. Pull requests are welcome.
