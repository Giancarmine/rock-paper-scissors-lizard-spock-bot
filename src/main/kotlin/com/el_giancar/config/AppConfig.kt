package com.el_giancar.config

/**
 * Runtime configuration read from the environment.
 *
 * The bot token is required; the HTTP port defaults to 8080 when `PORT` is absent
 * or not a number. The database settings are optional: without them the bot still
 * plays, but score tracking and leaderboards are disabled.
 */
data class AppConfig(
    val botToken: String,
    val port: Int,
    val databaseUrl: String? = null,
    val databaseUser: String? = null,
    val databasePassword: String? = null,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): AppConfig {
            val botToken =
                environment["BOT_TOKEN"]
                    ?.takeIf { it.isNotBlank() }
                    ?: error(
                        "BOT_TOKEN is not set. Create a bot with @BotFather and export the token " +
                            "before starting the application.",
                    )
            val port = environment["PORT"]?.toIntOrNull() ?: 8080
            val databaseUrl = environment["DATABASE_URL"]?.trim()?.takeIf { it.isNotEmpty() }
            val databaseUser = environment["DATABASE_USER"]?.trim()?.takeIf { it.isNotEmpty() }
            val databasePassword = environment["DATABASE_PASSWORD"]?.takeIf { it.isNotEmpty() }

            return AppConfig(
                botToken = botToken,
                port = port,
                databaseUrl = databaseUrl,
                databaseUser = databaseUser,
                databasePassword = databasePassword,
            )
        }
    }
}
