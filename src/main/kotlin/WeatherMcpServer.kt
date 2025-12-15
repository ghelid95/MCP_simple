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

            val latitude = request.arguments["latitude"]?.jsonPrimitive?.doubleOrNull
            val longitude = request.arguments["longitude"]?.jsonPrimitive?.doubleOrNull

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