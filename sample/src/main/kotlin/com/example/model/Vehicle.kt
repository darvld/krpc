package com.example.model

import kotlinx.serialization.Serializable

/**An abstract representation for a trackable vehicle.*/
@Serializable
data class Vehicle(
    /**The unique ID of this vehicle in the system.*/
    val id: Long,
    /**A string containing relevant information about this vehicle (manufacturer, model, etc.).*/
    val info: String
)
