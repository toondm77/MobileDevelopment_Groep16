package edu.ap.myapplication.model

data class Message(
    val from: String = "",
    val to: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    var fromName: String = ""
)

