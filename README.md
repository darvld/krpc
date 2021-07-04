# Kotlin RPC

KRPC is a more idiomatic variant of [GRPC-Kotlin](https://github.com/grpc/grpc-kotlin). Its goal is to provide more
concise ways to define and consume GRPC services in Kotlin, without using Protoc, while integrating seamlessly with the
kotlinx.serialization API.

This library consists of two components: the *compiler*, which processes service definitions and generates the
implementations; and the *runtime*, containing some necessary GRPC-Kotlin dependencies and the kotlinx.serialization
integration providers.

## Overview

Instead of using Protoc and .proto files to define services, KRPC uses Kotlin's interfaces and annotations. Any
interface annotated with `@Service` will be recognized as a definition by the KRPC compiler:

```kotlin
@Service
interface GpsService {
    @UnaryCall
    suspend fun locationForVehicle(vehicle: Vehicle): Location
}
```

The methods inside the service definitions should have the proper annotation to reflect the type of RPC call:

- `@UnaryCall` defines a single-request, single-response rpc method, it suspends until the server replies.
- `@ServerStream` takes a single request and returns a response `Flow`, which you can later use to consume the responses
  streamed by the server. Server-stream methods don't suspend, instead the returned flow collector will suspend until
  the first value is received.
- `@ClientStream` takes a request flow and returns a single response, suspending until it is received.
- `@BidiStream` takes a request flow and returns response flow, thus enabling bidirectional streaming. Like
  with `ServerStream`, this method does not suspend.

Method parameters and return types must be marked as `@Serializable`.

The compiler will then generate two implementations of this interface: an abstract service provider, and a client.
Additionally, another helper class is generated: the service's Descriptor, which contains all the generated method
descriptors and can be used to create your own custom client/server implementations. For a more detailed usage guide,
see [Getting Started](docs/Basic.md).

## Current state

KRPC is still in a very experimental state. Tests need to be written and features are still missing, so help is always
appreciated. Feel free to open an issue or submit a pull request if you have any suggestions or if you find a problem
with the library.

## Implementation

The KRPC compiler uses Google's [KSP](https://github.com/google/ksp) to process the annotations and generate code. The
runtime and generated stubs ae implemented on top of the [grpc-kotlin-stub](https://github.com/grpc/grpc-kotlin)
runtime.