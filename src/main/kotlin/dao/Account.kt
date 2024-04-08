package com.runerealms.auth.dao

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID

class Account(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<Account>(Accounts)
    var latestUsername by Accounts.latestUsername
    var password by Accounts.password
    var createdAt by Accounts.createdAt
    var updatedAt by Accounts.updatedAt
    var premium by Accounts.premiumAuth
}

object Accounts: UUIDTable() {
    val latestUsername = varchar("latest_username", 16)
    val password = varchar("password", 72)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val premiumAuth = bool("premium_auth").default(false)
}
