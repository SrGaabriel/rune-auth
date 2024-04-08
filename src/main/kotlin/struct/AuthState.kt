package com.runerealms.auth.struct

sealed class AuthState {
    data class Unauthenticated(
        var attempts: Int,
        var captcha: Boolean
    ): AuthState()

    data class Authenticated(
        val premium: Boolean
    ): AuthState()
}