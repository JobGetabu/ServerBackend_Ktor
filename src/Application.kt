package com.job

import com.job.auth.JwtService
import com.job.auth.MySession
import com.job.auth.hash
import com.job.auth.hashKey
import com.job.db.DatabaseFactory
import com.job.repository.TodoRepository
import com.job.routes.todos
import com.job.routes.users
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.locations.Locations
import io.ktor.response.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.sessions.*
import java.text.DateFormat

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(Locations) {
    }

    install(DefaultHeaders)
    install(Compression)
    install(CallLogging)

    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    // 1
    DatabaseFactory.init()
    val db = TodoRepository()
    // 2
    val jwtService = JwtService()
    val hashFunction = { s: String -> hash(s) }

    install(Authentication) {
        jwt("jwt") { //1
            verifier(jwtService.verifier) // 2
            realm = "Todo Server"
            validate { // 3
                val payload = it.payload
                val claim = payload.getClaim("id")
                val claimString = claim.asInt()
                val user = db.findUser(claimString) // 4
                user
            }
        }

    }



    routing {
        get("/"){
            call.respondText("Hello, world!")
        }

        get("/hello"){
            call.respondText("Hello, ktor!")
        }

        users(db, jwtService, hashFunction)

        todos(db)


        //testing
        application.log.debug("Secret key : $hashKey")

    }
}

const val API_VERSION = "/v1"