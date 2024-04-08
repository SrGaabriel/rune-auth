package com.runerealms.auth

import com.runerealms.auth.command.login
import com.runerealms.auth.command.register
import com.runerealms.auth.command.unregister
import com.runerealms.auth.dao.Accounts
import com.runerealms.auth.hash.HashingService
import com.runerealms.auth.listener.AuthListener
import com.runerealms.core.RunePlugin
import com.runerealms.core.feature.command.Commands
import com.runerealms.core.feature.database.Databases
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.async.AsyncListenerHandler
import com.runerealms.auth.captcha.CaptchaManager
import com.runerealms.auth.captcha.CategoryCaptchaManager
import com.runerealms.auth.command.premium
import com.runerealms.auth.listener.SkinListener
import com.runerealms.auth.minecraft.LoginManager
import com.runerealms.auth.minecraft.LoginSession
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.menu.Menus
import java.util.concurrent.ConcurrentHashMap

class RuneAuth: RunePlugin() {
    internal val hashingService = HashingService(salt="BCRYPT_SALT_MOCK")

    internal val protocolManager: ProtocolManager by lazy { ProtocolLibrary.getProtocolManager() }
    internal val loginSessions = ConcurrentHashMap<String, LoginSession>()
    internal val loginManager = LoginManager(this)

    var captchaManager: CaptchaManager? = CategoryCaptchaManager(this)
    val authenticationStates = ConcurrentHashMap<UUID, AuthState>()

    private lateinit var loginManagerHandler: AsyncListenerHandler

    override fun onStart() {
        install(Databases) {
            connectIntoCore()
            afterConnecting {
                transaction {
                    SchemaUtils.drop(Accounts)
                }
                tables(Accounts)
            }
        }
        install(Commands) {
            register(register)
            register(login)
            register(unregister)
            register(premium)
        }
        install(Menus)
        loginManagerHandler = protocolManager.asynchronousManager.registerAsyncHandler(loginManager)
        loginManagerHandler.start()

        Bukkit.getPluginManager().registerEvents(AuthListener(this), this)
        Bukkit.getPluginManager().registerEvents(SkinListener(this), this)
    }

    override fun onShutdown() {
        protocolManager.asynchronousManager.unregisterAsyncHandler(loginManager)
    }
}