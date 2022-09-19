package com.es.multivs.data.network.netmodels

data class ReportsMedication(
    val message: Message,
    val status: Boolean
) {
    data class Message(
        val medication: List<Medication>,
        val user_id: String
    ) {
        data class Medication(
            val asNeeded: String,
            val dosageForm: String,
            val dosageQuantity: String,
            val medicationId: Int,
            val medicationName: String,
            val status: String,
            val strength: String,
            val timestamp: String,
            val uid: Int
        )
    }
}