package dev.ru.ghelid

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class WeatherTool {
    private val logger = LoggerFactory.getLogger(WeatherTool::class.java)
    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getWeather(latitude: Double, longitude: Double): String {
        return try {
            logger.info("Fetching weather for coordinates: $latitude, $longitude")

            val pointsUrl = "https://api.weather.gov/points/$latitude,$longitude"
            logger.debug("Getting forecast URL from: $pointsUrl")

            val pointsResponse = client.get(pointsUrl) {
                header("User-Agent", "MCP-Weather-Server/1.0")
            }
            val pointsData = json.decodeFromString<PointsResponse>(pointsResponse.bodyAsText())
            val forecastUrl = pointsData.properties.forecast

            logger.debug("Fetching forecast from: $forecastUrl")
            val forecastResponse = client.get(forecastUrl) {
                header("User-Agent", "MCP-Weather-Server/1.0")
            }
            val forecastData = json.decodeFromString<ForecastResponse>(forecastResponse.bodyAsText())

            formatWeatherData(forecastData, latitude, longitude)
        } catch (e: Exception) {
            logger.error("Error fetching weather data", e)
            "Error fetching weather data: ${e.message}"
        }
    }

    private fun formatWeatherData(forecast: ForecastResponse, lat: Double, lon: Double): String {
        val periods = forecast.properties.periods.take(5)
        val result = buildString {
            appendLine("Weather Forecast for coordinates ($lat, $lon)")
            appendLine("=" .repeat(60))
            appendLine()

            for (period in periods) {
                appendLine("${period.name}:")
                appendLine("  Temperature: ${period.temperature}°${period.temperatureUnit}")
                appendLine("  Wind: ${period.windSpeed} ${period.windDirection}")
                appendLine("  Forecast: ${period.shortForecast}")
                appendLine("  Detailed: ${period.detailedForecast}")
                appendLine()
            }
        }
        return result
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class PointsResponse(
    val properties: PointsProperties
)

@Serializable
data class PointsProperties(
    val forecast: String
)

@Serializable
data class ForecastResponse(
    val properties: ForecastProperties
)

@Serializable
data class ForecastProperties(
    val periods: List<Period>
)

@Serializable
data class Period(
    val name: String,
    val temperature: Int,
    val temperatureUnit: String,
    val windSpeed: String,
    val windDirection: String,
    val shortForecast: String,
    val detailedForecast: String
)