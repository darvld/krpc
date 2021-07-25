# Working with gRPC metadata

When designing a service provider, we will need access to the caller's identity in order to retrieve information
necessary for the response, check access rights, etc. This information can be stored in gRPC metadata headers, provided
by client-side interceptors and later retrieved by server-side interceptors.

## Designing the metadata flow

Let's take a simple example: say we want to pass a session token as metadata from the client and retrieve that token
server-side, so we can check its validity. Finally, we will need to access some information about the caller inside our
server implementation. In order to do this we will need:

- A metadata key to serve as a global identifier for both interceptors, allowing to add/retrieve specific metadata from
  call headers.
- A client interceptor to add the session token to the metadata of all outgoing client calls.
- A server interceptor to retrieve the session token from the headers of incoming calls, validate the token and pass the
  relevant context information.
- A context key our server implementation will use to retrieve the context information passed by the interceptor.

Let's see how each one of these components are implemented:

## Metadata keys

Metadata headers act as key-value maps, storing values in a serialized form. In order to provide some degree of type
safety, gRPC uses keys to identity each particular header, so the metadata cannot be arbitrarily stored or retrieved
without using a pre-defined key.

All keys should be statically stored in such a way that it can be referenced by both interceptors. In this example we
will be using our service definition's companion object:

```kotlin
@Service
interface GpsService {
    // ... Our service methods ...

    companion object {
        /**Metadata used to validate client calls. Tokens are obtained from a separate Authorization service.*/
        val SESSION_TOKEN = metadataKey<String>("session_token", ProtoBuf.transcoder())
    }
}
```

The `metadataKey<T>` function creates a new key given a name and a `Transcoder` (in this case, one obtained using
the `BinaryFormat.transcoder<T>()` extension). Key names must contain only letters (`a-z`, `A-Z`), numbers (`0-9`) and
the special characters (`-`, `__`, `.`).

Metadata keys can have more complex types associated to them. For example, we might create a data class
named `SessionInfo`, containing the session's token and additional information. This is the correct way to store related
values in call headers. Even if the headers behave like a key-value map, it should not be treated that way, so it is
best to group closely related values in a single key. For this example, however, a simple string will be enough.

Now that our key has been created and stored statically, it's time to write the metadata interceptors.

## Client interceptor

> Although client interceptor allows to manipulate outgoing calls in several ways, this document will focus only on modifying the call's metadata headers.

Using the traditional grpc-java implementation, a simple client interceptor would look like this:

```kotlin
class ClientAuthInterceptor(val sessionToken: String) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>?, headers: Metadata) {
                headers.put(GpsService.SESSION_TOKEN, sessionToken)

                super.start(responseListener, headers)
            }
        }
    }
}
```

This is, to say the least, verbose. The `ClientMetadataInterceptor` base class provided by kRPC can help us make this
code readable:

```kotlin
/**A metadata interceptor used to provide the client's authorization token.*/
class ClientAuthInterceptor(val sessionToken: String) : ClientMetadataInterceptor() {
    override fun intercept(metadata: CallMetadata): CallMetadata {
        metadata.put(GpsService.SESSION_TOKEN, sessionToken)

        return metadata
    }
}
```

This looks a lot better, however, client interceptors will usually only *modify* the call's headers. Returning the same
metadata instance explicitly seems redundant and prone to error. Which is why for simple implementations we can use the
SAM constructor:

```kotlin
fun clientAuthInterceptor(val authToken: String) = ClientMetadataInterceptor { /* this: Metadata */
    put(GpsService.SESSION_TOKEN, authToken)
}
```

Now it's time to attach the interceptor to the client:

```kotlin
fun connectToGpsService(address: String, port: Int, sessionToken: String): GpsClient {
    val channel = ManagedChannelBuilder.forAddress(address, port).build()
    val interceptor = ClientAuthInterceptor(sessionToken)

    return GpsClient(channel, BinarySerializationProvider(ProtoBuf))
        .withInterceptors(interceptor)
}
```

## Server interceptor

Our server interceptor for this example will extract the session token from the headers and pass it as context to the
GPS server. We can create a new server interceptor by extending the `ServerMetadataInterceptor` base class:

```kotlin
class ServerAuthInterceptor : ServerMetadataInterceptor() {
    private fun authorize(token: String): boolean {
        // Let's keep it simple for the example
        return true
    }

    override fun intercept(context: Context, metadata: Metadata): Context {
        val token = metadata.get(GpsService.SESSION_TOKEN)?.takeIf { authorize(it) }
            ?: throw StatusRuntimeException(Status.UNAUTHENTICATED)

        return context
    }
}
```

Let's analyze this step by step:

- First, the token is obtained with `Metadata.get()` using our metadata key. The returned value is nullable, since there
  is no compile-time guarantee that the specified metadata is present in the headers.
- The token is validated using the `authorize` method. This is where the authentication logic should be, for this
  example we will accept all tokens (don't try this in production!).
- If there was no token in the headers, or if the validation fails, a new `StatusRuntimeException` is thrown
  with `UNAUTHENTICATED` as status code.
- Finally, the current, unaltered context is returned (more on this in the next section).

To attach the interceptor to the server, we can use two different approaches. The first one would be to intercept our
service provider:

```kotlin
fun startServer(port: Int): Server {
    val provider = GpsServer(BinarySerializationProvider(ProtoBuf))
    val intercepted = ServerInterceptors.intercept(provider, ServerAuthInterceptor())

    return ServerBuilder.forPort(port)
        .addService(intercepted)
        .build()
}
```

This will cause calls to be intercepted only if they are addressed to the GPS service. But let's say we have another
service providing some information about the vehicles we want to track. Certainly that service's provider would also
need to verify and access the caller's session token.

To avoid creating a separate interceptor for each service, we can do the following:

```kotlin
fun startServer(port: Int): Server {
    val serialProvider = BinarySerializationProvider(ProtoBuf)

    return ServerBuilder.forPort(SERVER_PORT)
        .intercept(ServerAuthInterceptor)
        .addService(GpsServer(serialProvider))
        .addService(InfoServer(serialProvider))
        .build()
}
```

Now all calls will be verified by our interceptor, and every service running on this server will have access to any
context information provided by `ServerAuthInterceptor`.

Up to this point, the authorization functionality should work properly, however we still need a way to access the
token's value from the service provider's implementation.

## Context keys

Within the server, every call is processed within a `CallContext`, which allows us to pass information between the
interceptor and the service provider, using context keys. Like metadata keys, context keys should be stored statically
so they can be accessed by both the server interceptor and the service provider:

```kotlin
class GpsServer(serializationProvider: SerializationProvider) : GpsServiceProvider(serializationProvider) {
    // ... our service implementation ...

    companion object {
        val SessionToken = contextKey<String>("session_token")
    }
}
```

The `contextKey<T>` function returns a new key, and uses the string argument for debugging purposes. Note that there is
no need to match any of the properties between context and metadata keys. Once the key is created, we can use our server
interceptor to associate values within a context:

```kotlin
class ServerAuthInterceptor : ServerMetadataInterceptor() {
    private fun authorize(token: String): boolean {
        // Let's keep it simple for the example
        return true
    }

    override fun intercept(context: Context, metadata: Metadata): Context {
        val token = metadata.get(GpsService.SESSION_TOKEN)?.takeIf { authorize(it) }
            ?: throw StatusRuntimeException(Status.UNAUTHENTICATED)

        // Context.withValue returns a new context containing the associated key-value pair
        return context.withValue(GpsServer.SessionToken, token)
    }
}
```

To retrieve the value from the service provider, use `ContextKey.get()`:

```kotlin
override suspend fun handshake() {
    val token: String = SessionToken.get()
    println("[Server] User with token=$token has connected to the service.")
}
```

Done! Now all calls made to the server will be validated, and our service will have access to the caller's token for
additional context information.

## Server-side exception handling

When implementing a server interceptor or a service provider, often we will need to cancel the current call (due to
invalid credentials, illegal state issues, etc.). However, there is no way for us to directly access the current call.
Instead we can throw a `StatusRuntimeException` and set the status code to match the situation.

This will cause the current call to fail with the appropriate HTTP error code, it will not shutdown the server or the
interceptor. If you are familiar with kotlin coroutines, the `CancellationException` bears some similarities, in that it
cancels the current scope without crashing the entire program. Since Java does not have coroutines, we need to throw
a  `StatusRuntimeException` in order to cancel a call. If any other exception is thrown during interception, it will be
automatically wrapped with status code  `UNKNOWN`.

## Client-side exception handling

Whenever a call fails, the service method invocation will throw the corresponding `StatusRuntimeException`. Unlike the
server, the client will not handle this automatically, so client calls should be surrounded with the appropriate
try-catch blocks if needed.