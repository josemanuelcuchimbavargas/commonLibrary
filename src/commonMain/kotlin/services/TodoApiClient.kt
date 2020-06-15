package services

import error.ApiError
import error.UnknownError
import error.ItemNotFoundError
import error.NetworkError
import model.Task
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.BadResponseStatusException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import todoapiclient.Either

class TodoApiClient constructor(
    httpClientEngine: HttpClientEngine? = null
){

    companion object{
        const val BASE_ENDPOINT = "http://jsonplaceholder.typicode.com"
    }

    private val client: HttpClient = HttpClient(httpClientEngine!!) {
        install(JsonFeature) {
            serializer = KotlinxSerializer().apply {
                register(Task.serializer())
            }
        }
    }

    suspend fun getAllTasks(): Either<ApiError, List<Task>> = try {
        val tasksJson = client.get<String>("$BASE_ENDPOINT/todos")

        val tasks = Json.nonstrict.parse(Task.serializer().list, tasksJson)

        Either.Right(tasks)

    } catch (e: Exception) {
        handleError(e)
    }

    suspend fun getTasksById(id: String): Either<ApiError, Task> = try {
        val task = client.get<Task>("$BASE_ENDPOINT/todos/$id")

        Either.Right(task)
    } catch (e: Exception) {
        handleError(e)
    }

    suspend fun addTask(task: Task): Either<ApiError, Task> = try {
        val taskResponse = client.post<Task>("$BASE_ENDPOINT/todos") {
            contentType(ContentType.Application.Json)
            body = task
        }

        Either.Right(taskResponse)
    } catch (e: Exception) {
        handleError(e)
    }

    suspend fun updateTask(task: Task): Either<ApiError, Task> = try {
        val taskResponse = client.put<Task>("$BASE_ENDPOINT/todos/${task.id}") {
            contentType(ContentType.Application.Json)
            body = task
        }

        Either.Right(taskResponse)
    } catch (e: Exception) {
        handleError(e)
    }

    suspend fun deleteTask(id: String): Either<ApiError, Boolean> = try {
        client.delete<String>("$BASE_ENDPOINT/todos/$id")

        Either.Right(true)
    } catch (e: Exception) {
        handleError(e)
    }

    private fun handleError(exception: Exception): Either<ApiError, Nothing> =
        if (exception is BadResponseStatusException) {
            if (exception.statusCode.value == 404) {
                Either.Left(ItemNotFoundError)
            } else {
                Either.Left(UnknownError(exception.statusCode.value))
            }
        } else {
            Either.Left(NetworkError)
        }

}
