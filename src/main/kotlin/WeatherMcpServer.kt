package dev.ru.ghelid

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.*
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered

class WeatherMcpServer {
    private val logger = LoggerFactory.getLogger(WeatherMcpServer::class.java)
    private val weatherTool = WeatherTool()
    private val taskTool = TaskTool()

    suspend fun start() {
        logger.info("Starting Weather MCP Server...")

        val server = Server(
            serverInfo = Implementation(
                name = "weather-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        server.addTool(
            name = "get_weather",
            description = "Get weather forecast from the National Weather Service (weather.gov) for a specific location. Provide latitude and longitude coordinates to get the current weather forecast.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("latitude") {
                        put("type", "number")
                        put("description", "Latitude coordinate (e.g., 39.7456 for Denver)")
                    }
                    putJsonObject("longitude") {
                        put("type", "number")
                        put("description", "Longitude coordinate (e.g., -97.0892 for Denver)")
                    }
                },
                required = listOf("latitude", "longitude")
            )
        ) { request ->
            logger.debug("Handling get_weather request")

            val latitude = request.arguments?.get("latitude")?.jsonPrimitive?.doubleOrNull
            val longitude = request.arguments?.get("longitude")?.jsonPrimitive?.doubleOrNull

            if (latitude == null || longitude == null) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Error: Both latitude and longitude must be provided as numbers"
                        )
                    ),
                    isError = true
                )
            }

            logger.info("Getting weather for: $latitude, $longitude")
            val weatherData = weatherTool.getWeather(latitude, longitude)

            CallToolResult(
                content = listOf(
                    TextContent(text = weatherData)
                )
            )
        }

        server.addTool(
            name = "add_task",
            description = "Add a new task with name, description, and due date. Tasks are saved to disk in JSON format.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Name of the task")
                    }
                    putJsonObject("description") {
                        put("type", "string")
                        put("description", "Detailed description of the task")
                    }
                    putJsonObject("dueDate") {
                        put("type", "string")
                        put("description", "Due date in YYYY-MM-DD format (e.g., 2025-12-31)")
                    }
                },
                required = listOf("name", "description", "dueDate")
            )
        ) { request ->
            logger.debug("Handling add_task request")

            val name = request.arguments?.get("name")?.jsonPrimitive?.contentOrNull
            val description = request.arguments?.get("description")?.jsonPrimitive?.contentOrNull
            val dueDate = request.arguments?.get("dueDate")?.jsonPrimitive?.contentOrNull

            if (name == null || description == null || dueDate == null) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Error: name, description, and dueDate must all be provided"
                        )
                    ),
                    isError = true
                )
            }

            logger.info("Adding task: $name")
            val result = taskTool.addTask(name, description, dueDate)

            CallToolResult(
                content = listOf(
                    TextContent(text = result)
                )
            )
        }

        server.addTool(
            name = "search_tasks",
            description = "Search through saved tasks. Returns all tasks if no filter is provided, or filters by due date if specified.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("dueDate") {
                        put("type", "string")
                        put("description", "Optional: Filter tasks by due date in YYYY-MM-DD format (e.g., 2025-12-31)")
                    }
                },
                required = listOf()
            )
        ) { request ->
            logger.debug("Handling search_tasks request")

            val dueDate = request.arguments?.get("dueDate")?.jsonPrimitive?.contentOrNull

            logger.info("Searching tasks with dueDate: $dueDate")
            val result = taskTool.searchTasks(dueDate)

            CallToolResult(
                content = listOf(
                    TextContent(text = result)
                )
            )
        }

        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )

        server.connect(transport)

        logger.info("Weather MCP Server is running and listening on stdio")
    }

    fun close() {
        weatherTool.close()
    }
}