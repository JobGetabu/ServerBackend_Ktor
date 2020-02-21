package com.job.routes

import com.job.API_VERSION
import com.job.auth.JwtService
import com.job.auth.MySession
import com.job.repository.Repository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set

const val TODOS = "$API_VERSION/todos"

@KtorExperimentalLocationsAPI
@Location(TODOS)
class TodoRoute

@KtorExperimentalLocationsAPI
fun Route.todos(db: Repository) {
    authenticate("jwt") { // 1
        post<TodoRoute> { // 2
            val todosParameters = call.receive<Parameters>()
            val todo = todosParameters["todo"]
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest, "Missing Todo")
            val done = todosParameters["done"] ?: "false"
            // 3
            val user = call.sessions.get<MySession>()?.let {
                db.findUser(it.userId)
            }
            if (user == null) {
                call.respond(
                    HttpStatusCode.BadRequest, "Problems retrieving User")
                return@post
            }

            try {
                // 4
                val currentTodo = db.addTodo(
                    user.userId, todo, done.toBoolean())
                currentTodo?.id?.let {
                    call.respond(HttpStatusCode.OK, currentTodo)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to add todo", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Saving Todo")            }
        }
    }

    get<TodoRoute> {
        val user = call.sessions.get<MySession>()?.let { db.findUser(it.userId) }
        if (user == null) {
            call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
            return@get
        }
        try {
            val todos = db.getTodos(user.userId)
            call.respond(todos)
        } catch (e: Throwable) {
            application.log.error("Failed to get Todos", e)
            call.respond(HttpStatusCode.BadRequest, "Problems getting Todos")
        }
    }

}
