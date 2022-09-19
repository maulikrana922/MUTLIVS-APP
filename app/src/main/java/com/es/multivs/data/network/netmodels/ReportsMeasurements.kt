package com.es.multivs.data.network.netmodels

data class ReportsMeasurements(
    val message: List<Message>,
    val status: Boolean
) {
    data class Message(
        val blood_glucose: Float,
        val body_temperature: Float,
        val bp_diastolic: String,
        val bp_systolic: String,
        val heart_beat: Float,
        val respiratory_rate:Float,
        val rrt: Float,
        val spo2: Float,
        val step_value: Float,
        val timestamp: String,
        val user_id: Int,
        val weight: Float
    )
}