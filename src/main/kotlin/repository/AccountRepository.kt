package com.runerealms.auth.repository

import com.runerealms.auth.dao.Account
import java.util.UUID

interface AccountRepository {
    fun createAccount(username: String, password: String): Boolean
    fun findAccount(uuid: UUID): Account?
    @Deprecated("Use findAccount instead")
    fun findAccountByName(username: String): Account?
    fun changePassword(username: String, password: String): Boolean
    fun deleteAccount(username: String): Boolean
}