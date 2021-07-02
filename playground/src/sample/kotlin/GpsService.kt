import com.github.darvld.krpc.*
import kotlinx.coroutines.flow.Flow

// @Service
interface GpsService {
    @UnaryCall
    suspend fun locationForVehicle(vehicle: Vehicle): Location

    @ServerStream
    fun routeForVehicle(vehicle: Vehicle): Flow<Location>

    @ClientStream
    suspend fun streamRoute(route: Flow<Location>): Boolean

    @BidiStream
    fun transformRoute(route: Flow<Location>): Flow<Location>
}