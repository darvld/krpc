# Getting started with KRPC

In order to define and then generate a gRPC service, you would normally need to define a ProtoBuf (.proto)
definition, including the requests, responses and all necessary data structures. Then you would use the Protoc compiler
with the gRPC plugin (Java or Kotlin variant) to generate some code you can consume. However, this process generates a
lot of exposed boilerplate, namely builders, DSLs for those builders and other utilities.

With kRPC the approach is simpler: you define your service *in Kotlin* as an interface, and then use the kRPC processor
to generate the server and client implementations.

## Adding kRPC to your project

Simply apply Google's KSP gradle plugin and add kRPC as a dependency:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.5.10'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.5.10'

    id "com.google.devtools.ksp" version "1.5.10-1.0.0-beta02"
}

dependencies {
    // Runtime, containing the annotations and other utils
    implementation "com.github.darvld.krpc:krpc-runtime:$krpcVersion"

    // A serialization format is needed to instantiate the client and the server
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion"
    // A transport for the GRPC-Kotlin runtime
    implementation "io.grpc:grpc-netty:$grpcVersion"

    // Workaround for a gradle-related KSP bug
    configurations.ksp.dependencies.add(project.dependencies.create("com.github.darvld.krpc:krpc-compiler:$krpcVersion"))
}
```

## A simple service definition

Let's say we need to define a GPS service to provide information on the location of a certain vehicle to remote clients.
First, we define our serializable model:

```kotlin
@Serializable
data class Vehicle(val id: Long, val model: String)

@Serializable
data class Location(val latitude: Double, val longitude: Doubles)
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

Now compile your project. The compiler will generate a service provider and a client implementation. However, due to
limitations in KSP, the generated sources will not be automatically detected by the IDE. In order to use them properly,
add the following to your `build.gradle`:

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

You must be wondering by now what the `SerializationProvider` does and why it is required to construct the provider. In
fact, the `SerializationProvider` is an abstraction used to plug in to the kotlinx.serialization API. To use it, create
a custom provider:

```kotlin
@OptIn(ExperimentalSerializationApi::class)
object ProtoBufSerializationProvider : SerializationProvider {
    override fun <T> marshallerFor(serializer: KSerializer<T>): MethodDescriptor.Marshaller<T> {
        return object : MethodDescriptor.Marshaller<T> {
            override fun parse(stream: InputStream): T {
                return ProtoBuf.decodeFromByteArray(serializer, stream.readAllBytes())
            }

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
    val server = ServerBuilder.forPort(8980)
        .addService(GpsServer(ProtoBufSerializationProvider))
        .build()

    server.start()

    val channel = ManagedChannelBuilder.forAddress("localhost", 8980).build()
    val client = GpsClient(channel, ProtoBufSerializationProvider)
    
    val vehicle = Vehicle(id=1438, model="Foo")
    val location = client.locationForVehicle(vehicle)
    
    println("According to the server, $vehicle is currently at $location")

    server.shutdown()
    server.awaitTermination()
}
```
