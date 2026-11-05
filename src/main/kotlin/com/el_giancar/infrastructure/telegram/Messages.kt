package com.el_giancar.infrastructure.telegram

internal val rulesMessage: String = "The rules are pretty simple which you could even memorize \uD83E\uDDE0 \n\n" +
        "- Scissors cuts Paper \n" +
        "- Paper covers Rock \n" +
        "- Rock crushes Lizard \n" +
        "- Lizard poisons Spock\n" +
        "- Spock smashes Scissors\n" +
        "- Scissors decapitates Lizard\n" +
        "- Lizard eats Paper\n" +
        "- Paper disproves Spock\n" +
        "- Spock vaporizes Rock\n" +
        "- (and as it always has) Rock crushes Scissors"

internal val helpMessage: String = """
    |Available commands:
    |/classic_game - play with Rock, Paper, Scissors
    |/sheldon_game - play with Rock, Paper, Scissors, Lizard, Spock
    |/rules - show the rules
    |/commands - show the command keyboard
    |/about - about this bot
    |
    |During a game, send any command to cancel it.
""".trimMargin()

internal val aboutMessage: String = """
    |I'm a small open-source Telegram bot for Rock-Paper-Scissors and
    |Rock-Paper-Scissors-Lizard-Spock.
    |
    |Built with Kotlin, Ktor and tgbotapi.
    |Source: https://github.com/Giancarmine/rock-paper-scissors-lizard-spock-bot
""".trimMargin()
