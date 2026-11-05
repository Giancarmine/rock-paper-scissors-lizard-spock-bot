package com.el_giancar.config

/**
 * Runtime configuration read from the environment.
 *
 * The bot token is required; the HTTP port defaults to 8080 when `PORT` is absent
 * or not a number.
 */
data class AppConfig(
    val botToken: String,
    val port: Int,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): AppConfig {
            val botToken = environment["BOT_TOKEN"]
                ?.takeIf { it.isNotBlank() }
                ?: error(
                    "BOT_TOKEN is not set. Create a bot with @BotFather and export the token " +
                        "before starting the application."
                )
            val port = environment["PORT"]?.toIntOrNull() ?: 8080

            return AppConfig(botToken = botToken, port = port)
        }
    }
}
