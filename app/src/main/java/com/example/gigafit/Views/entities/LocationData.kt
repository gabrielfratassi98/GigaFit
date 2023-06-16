package com.example.gigafit.Views.entities

import androidx.room.Entity

@Entity
data class LocationData(
    var latitude: Double,
    var longitude: Double,
    var timestamp: Long,
)