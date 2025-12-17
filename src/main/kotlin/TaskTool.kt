package dev.ru.ghelid

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class Task(
    val name: String,
    val description: String,
    val dueDate: String // ISO date format: YYYY-MM-DD
)

class TaskTool {
    private val logger = LoggerFactory.getLogger(TaskTool::class.java)
    private val json = Json { prettyPrint = true }
    private val tasksFile = File("tasks.json")

    fun addTask(name: String, description: String, dueDate: String): String {
        return try {
            logger.info("Adding new task: $name")

            // Validate date format
            try {
                LocalDate.parse(dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return "Error: Invalid date format. Please use YYYY-MM-DD format (e.g., 2025-12-31)"
            }

            val newTask = Task(name, description, dueDate)

            // Read existing tasks
            val tasks = if (tasksFile.exists()) {
                val content = tasksFile.readText()
                if (content.isNotBlank()) {
                    json.decodeFromString<List<Task>>(content).toMutableList()
                } else {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            // Add new task
            tasks.add(newTask)

            // Save to file
            tasksFile.writeText(json.encodeToString(tasks))

            logger.info("Task added successfully: $name")
            buildString {
                appendLine("Task added successfully!")
                appendLine()
                appendLine("Name: ${newTask.name}")
                appendLine("Description: ${newTask.description}")
                appendLine("Due Date: ${newTask.dueDate}")
                appendLine()
                appendLine("Total tasks: ${tasks.size}")
                appendLine("Saved to: ${tasksFile.absolutePath}")
            }
        } catch (e: Exception) {
            logger.error("Error adding task", e)
            "Error adding task: ${e.message}"
        }
    }

    fun searchTasks(dueDate: String? = null): String {
        return try {
            logger.info("Searching tasks with dueDate filter: $dueDate")

            // Check if tasks file exists
            if (!tasksFile.exists()) {
                return "No tasks found. The tasks file doesn't exist yet."
            }

            // Read tasks from file
            val content = tasksFile.readText()
            if (content.isBlank()) {
                return "No tasks found. The tasks file is empty."
            }

            val allTasks = json.decodeFromString<List<Task>>(content)

            // Filter tasks by due date if provided
            val filteredTasks = if (dueDate != null) {
                // Validate date format
                try {
                    LocalDate.parse(dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    return "Error: Invalid date format. Please use YYYY-MM-DD format (e.g., 2025-12-31)"
                }
                allTasks.filter { it.dueDate == dueDate }
            } else {
                allTasks
            }

            if (filteredTasks.isEmpty()) {
                return if (dueDate != null) {
                    "No tasks found with due date: $dueDate"
                } else {
                    "No tasks found."
                }
            }

            // Format the results
            buildString {
                if (dueDate != null) {
                    appendLine("Tasks with due date: $dueDate")
                } else {
                    appendLine("All Tasks")
                }
                appendLine("=".repeat(60))
                appendLine()
                appendLine("Total tasks found: ${filteredTasks.size}")
                appendLine()

                filteredTasks.forEachIndexed { index, task ->
                    appendLine("Task ${index + 1}:")
                    appendLine("  Name: ${task.name}")
                    appendLine("  Description: ${task.description}")
                    appendLine("  Due Date: ${task.dueDate}")
                    appendLine()
                }
            }
        } catch (e: Exception) {
            logger.error("Error searching tasks", e)
            "Error searching tasks: ${e.message}"
        }
    }
}
