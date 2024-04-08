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
import com.runerealms.auth.dao.Account
import com.runerealms.auth.listener.SkinListener
import com.runerealms.auth.minecraft.LoginManager
import com.runerealms.auth.minecraft.LoginSession
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.config.createLocale
import com.runerealms.core.feature.database.repository.CachedSqlRepository
import com.runerealms.core.feature.menu.Menus
import io.github.reactivecircus.cache4k.Cache
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

class RuneAuth: RunePlugin() {
    internal val hashingService = HashingService(salt="BCRYPT_SALT_MOCK")

    internal val protocolManager: ProtocolManager by lazy { ProtocolLibrary.getProtocolManager() }
    internal val loginSessions = ConcurrentHashMap<String, LoginSession>()
    internal val loginManager = LoginManager(this)

    var captchaManager: CaptchaManager = CategoryCaptchaManager(this)
    val authenticationStates = ConcurrentHashMap<UUID, AuthState>()

    val accountRepository = CachedSqlRepository(
        Account,
        cacheBuilder = Cache.Builder<UUID, Account>()
            .maximumCacheSize(1000)
            .expireAfterAccess(20.minutes)
    )

    private lateinit var loginManagerHandler: AsyncListenerHandler

    override fun onStart() {
        install(Databases) {
            connectIntoCore()
            afterConnecting {
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

//        val commandLogRegex = Regex("""(\w+) issued server command: /(.+)""")
//        Bukkit.getServer().logger.setFilter {
//            // With regex, let's check if it is like this `X issued server command: /{command}`, and return the command
//            val match = commandLogRegex.matchEntire(it.message)
//            val player = match?.groupValues?.get(1)
//            val command = match?.groupValues?.get(2)
//            if (player != null && command != null) {
//                val authCommand = feature(Commands).repository.search(command)
//                if (authCommand != null) {
//                    it.message = "$player issued server command: /$command"
//                }
//            }
//            true
//        }

        makeLocale()
    }

    private fun makeLocale() {
        createLocale("messages") {
            this["commands.not-logged"] = "&c&lERRO &fVocê não está autenticado no servidor."
            this["commands.already-logged"] = "&c&lERRO &fVocê já está autenticado no servidor."
            this["commands.already-registered"] = "&c&lERRO &fVocê já tem uma conta cadastrada no servidor."
            this["commands.no-account"] = "&c&lERRO &fVocê não tem uma conta. Use &c/registrar <senha>&f para criar uma."
            this["commands.premium-tip"] = "&9&lDICA &fUsuários com Minecraft original não precisam se autenticar. Use &9/original &fpara ativar."
            this["commands.incorrect-password"] = "&c&lERRO &fSenha incorreta."
            this["commands.attempts-left"] = "&fVocê tem mais &c{0} &ftentativas."
            this["commands.successful-login"] = "&a&lSUCESSO &fVocê foi autenticado com sucesso."
            this["commands.successful-register"] = "&a&lSUCESSO &fConta registrada com sucesso."
            this["commands.register.password-too-short"] = "&c&lERRO &fA senha precisa ter no mínimo &c{0} &fcaracteres."
            this["commands.register.password-too-long"] = "&c&lERRO &fA senha precisa ter no máximo &c{0} &fcaracteres."
            this["commands.premium.enabled"] = "&6&lATIVADO &fVocê &6ativou &fo modo de autenticação original."
            this["commands.premium.disabled"] = "&c&lDESATIVADO &fVocê &cdesativou &fo modo de autenticação original."
            this["commands.premium.non-premium"] = "&c&lERRO &fVocê não está logado com uma conta original."
            this["events.migration"] = "&3&lMIGRAÇÃO &fSeus dados da sua conta anterior &3{0} &fforam migrados com sucesso!"
            this["events.command-blocked"] = "&c&lERRO &fVocê precisa estar autenticado para usar comandos."
            this["events.chat-blocked"] = "&c&lERRO &fVocê precisa estar autenticado para falar no chat."
            this["kick.account-cancelled"] = "&c&lCANCELAMENTO\n&n\n&fSua conta foi cancelada."
            this["kick.excessive-attempts"] = "&c&lLIMITE EXCEDIDO\n&n\n&fVocê excedeu o limite de tentativas de login."
            this["kick.captcha-fail"] = "&c&lERRO\n&n\n&fO captcha foi respondido incorretamente."
            this["kick.login-timeout"] = "&c&lERRO\n&n\n&fVocê demorou muito tempo para logar."
            this["titles.login.title"] = "&a&lLOGIN"
            this["titles.login.subtitle"] = "&fSeja bem-vindo novamente!"
            this["titles.register.title"] = "&a&lREGISTRADO"
            this["titles.register.subtitle"] = "Você se registrou ao servidor com sucesso!"
            this["titles.ask-login.title"] = "&e&lLOGIN"
            this["titles.ask-login.subtitle"] = "&fFaça seu login utilizando &e/login <senha>&f para começar a jogar."
            this["titles.ask-register.title"] = "&e&lREGISTRO"
            this["titles.ask-register.subtitle"] = "&fFaça seu cadastro utilizando &e/registrar <senha>&f para começar a jogar."
            this["titles.premium.title"] = "&6&lORIGINAL"
            this["titles.premium.subtitle"] = "&fSua conta é &6original&f, você não precisa se autenticar."
        }
    }

    override fun onShutdown() {
        protocolManager.asynchronousManager.unregisterAsyncHandler(loginManager)
    }
}