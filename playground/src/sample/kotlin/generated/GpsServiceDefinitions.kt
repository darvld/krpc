package generated

import Location
import SerializationProvider
import Vehicle
import io.grpc.MethodDescriptor
import kotlinx.serialization.builtins.serializer

class GpsServiceDefinitions(serializationProvider: SerializationProvider) {
    private val vehicleMarshaller = serializationProvider.marshallerFor(Vehicle.serializer())
    private val locationMarshaller = serializationProvider.marshallerFor(Location.serializer())

    val locationForVehicle: MethodDescriptor<Vehicle, Location> = MethodDescriptor.newBuilder<Vehicle, Location>()
        .setFullMethodName("GpsService/locationForVehicle")
        .setType(MethodDescriptor.MethodType.UNARY)
        .setRequestMarshaller(vehicleMarshaller)
        .setResponseMarshaller(locationMarshaller)
        .build()

    val routeForVehicle: MethodDescriptor<Vehicle, Location> = MethodDescriptor.newBuilder<Vehicle, Location>()
        .setFullMethodName("GpsService/routeForVehicle")
        .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
        .setRequestMarshaller(vehicleMarshaller)
        .setResponseMarshaller(locationMarshaller)
        .build()

    val streamRoute: MethodDescriptor<Location, Boolean> = MethodDescriptor.newBuilder<Location, Boolean>()
        .setFullMethodName("GpsService/streamRoute")
        .setType(MethodDescriptor.MethodType.CLIENT_STREAMING)
        .setRequestMarshaller(locationMarshaller)
        .setResponseMarshaller(serializationProvider.marshallerFor(Boolean.serializer()))
        .build()

    val transformRoute: MethodDescriptor<Location, Location> = MethodDescriptor.newBuilder<Location, Location>()
        .setFullMethodName("GpsService/transformRoute")
        .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
        .setRequestMarshaller(locationMarshaller)
        .setResponseMarshaller(locationMarshaller)
        .build()
}