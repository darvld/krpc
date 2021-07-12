# Roadmap

This project is still in early development, and while most of the public API is stable and tested, it has yet to be used in production or even in experiments to really earn the 'stable' title. We encourage you to try it out and provide some feedback on your experiences. If you have any suggestions about features you would like to see implemented, you can [open an issue]() or [submit a pull request](). Contributions are welcome!

Below are some of the main features that will be implemented in future versions. Some of them rely on other frameworks and libraries and might take a while, and others will be added soon. This list does not contain *every* feature to be implemented, but should provide you with an idea of where the project is moving towards.

## Implemented features

The following features have already been implemented and can be considered as stable, that is, the public API providing these features should not change in a backwards-incompatible manner in the following releases.

- Unary, client-streaming, server-streaming and bidi-streaming calls.
- Flexibility for arguments and return types in declarations.
  - Multiple/zero arguments in `@UnaryCall` and `@ServerStream` methods.
  - Omitted return type for `@UnaryCall` and `@ClientStream` methods.
- Automatic generation of clients and abstract servers to be used in general code.
- Integration with `kotlinx.serialization`, flexible serial format.
- Support for Kotlin Multiplatform projects (for now, JVM targets only).
  - Services can be defined in the Common module and manually implemented on non-JVM targets.
- Custom names for methods/services in gRPC (through annotation parameters) and for generated components.
- Coroutine extensions for  `Server` and `ManagedChannel `on JVM.

## Under development

The following features will be implemented in the future. The order in which they appear does not signal anything special. More features might be added to this list eventually and others might be removed (if they are no longer needed).

- Compile-time check of request/response types to ensure they are serializable.
- Selective component code generation (client-only, server-only, descriptor-only modes).
- Extensions for the gRPC Metadata API.
- Extracting `.proto` declarations from Kotlin `@Service` interfaces (to allow interoperability with other languages).

## Suspended

Features listed below will be implemented in the future, once certain issues in this project's dependencies are resolved.

- Kotlin/JS and Kotlin/Native targets (depends on KSP's support for these targets, see the corresponding [issue](https://github.com/google/ksp/issues/33)).

