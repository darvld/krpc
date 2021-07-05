package com.github.darvld.krpc

/**Marker for KRPC service definitions. This annotation serves as an entry point for the KRPC symbol processor.
 *
 * Service definitions must be interfaces containing only methods annotated with @[UnaryCall], @[ServerStream],
 * @[ClientStream] or @[BidiStream], and complying with the corresponding signatures. Any other method will cause
 * a processing error.*/
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Service(val overrideName: String = "", val providerName: String = "", val clientName: String = "")