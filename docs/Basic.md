# Getting started with kRPC

This document covers the basics of kRPC: setting up a project, applying the compiler and creating a simple
service definition. You can check out all the library features in the [Sample project](https://github.com/darvld/krpc/tree/main/sample). More details on service definitions can be found [here](Advanced.md).

In order to define and then generate a gRPC service, you would normally need to define a ProtoBuf (.proto)
definition, including the requests, responses and all necessary data structures. Then you would use the Protoc compiler with a gRPC plugin to generate some code you can consume. However, this process generates a
lot of exposed boilerplate, namely builders, DSLs for those builders and other utilities, which are a bit unnecessary in Kotlin.

With kRPC the approach is simpler: you define your service *in Kotlin* as an interface, and then use the kRPC processor
to generate the server and client implementations.

## Adding kRPC to your project

Simply apply Google's KSP gradle plugin and add kRPC as a dependency:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.5.20'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.5.20'

    id "com.google.devtools.ksp" version '1.5.20-1.0.0-beta04'
}

dependencies {
    // Runtime, containing the annotations and other utils
    implementation "com.github.darvld.krpc:krpc-runtime:$krpcVersion"

    // A serial format is needed to provide runtime serialization
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion"
  
    // A transport for the grpc-kotlin runtime
    implementation "io.grpc:grpc-netty:$grpcVersion"

    // Apply the kRPC compiler using the ksp plugin
    ksp "io.github.darvld.krpc:krpc-compiler:$krpcVersion"
}
```

## A simple service definition

Let's say we need to define a GPS service to provide information on the location of a certain vehicle to remote clients.
First, we define our serializable model:

```kotlin
@Serializable
data class Vehicle(val id: Long, val model: String)

@Serializable
data class Location(val latitude: Double, val longitude: Double)
```

Then we could define our service like this:

```kotlin
@Service
interface GpsService {
    @UnaryCall
    suspend fun locationForVehicle(vehicle: Vehicle): Location

    @ServerStream
    fun routeForVehicle(vehicle: Vehicle): Flow<Location>
}
```

## Generating the code

You can use the `kspKotlin` gradle task to run the processor and trigger code generation. The compiler will generate a
service provider and a client implementation.

Currently, due to limitations in KSP, the generated sources will not be automatically detected by the IDE.
In order to use them properly, add the following to your `build.gradle`:

```groovy
sourceSets {
    main {
        kotlin.srcDir(file("build/generated/ksp/main/kotlin"))
    }
}
```

## Using the generated sources

Now we can create a custom GPS service provider using the generated base class:

```kotlin
class GpsServer(
    serializationProvider: SerializationProvider,
    private val backend: GpsBackend
) : GpsServiceProvider(serializationProvider) {

    override suspend fun locationForVehicle(vehicle: Vehicle): Location {
        return backend.findVehicle(vehicle)
    }

    override fun routeForVehicle(vehicle: Vehicle): Flow<Location> {
        return backend.trackVehicle(vehicle)
    }
}
```

You must be wondering by now what the `SerializationProvider` does and why it is required to construct the provider.
The `SerializationProvider` API is an abstraction used to plug in to the kotlinx-serialization API, in JVM specifically,
it produces gRPC `Marshaller<T>` instances to marshall the requests and responses of rpc methods. It's easy
to create a provider using any serial format from kotlinx-serialization:

```kotlin
@OptIn(ExperimentalSerializationApi::class)
object ProtoBufSerializationProvider : SerializationProvider {
  override fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T> {
    return object : MethodDescriptor.Marshaller<T> {
      // Decode a serializable value from an InputStream
      override fun parse(stream: InputStream): T {
        return ProtoBuf.decodeFromByteArray(serializer, stream.readAllBytes())
      }
			
      // Encode a serializable value and provide an InputStream to read it
      override fun stream(value: T): InputStream {
        return ByteArrayInputStream(ProtoBuf.encodeToByteArray(serializer, value))
      }
    }
  }
}
```

Now we can instantiate a server and a client:

```kotlin
fun main() = runBlocking {
    val server = ServerBuilder.forPort(8080)
        .addService(GpsServer(ProtoBufSerializationProvider))
        .build()

    server.start()

  	// A ManagedChannel provides lifecycle control methods (like shutdown)
    val channel = ManagedChannelBuilder
        .forAddress("localhost", 8080)
        .usePlainText()
        .build()
    
    val client = GpsClient(channel, ProtoBufSerializationProvider)
    
    val vehicle = Vehicle(id=1438, model="Foo")
    val location = client.locationForVehicle(vehicle)
    
    println("According to the server, $vehicle is currently at $location")
    
    client.shutdownAndJoin()
    server.shutdownAndJoin()
}
```

## Next steps

Now we have a working implementation of a gRPC service, ready to add more methods and functionalities. For more details on how to declare services and methods, see [this guide](Advanced.md).

