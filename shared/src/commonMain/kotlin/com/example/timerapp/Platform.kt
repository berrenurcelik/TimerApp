package com.example.timerapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform