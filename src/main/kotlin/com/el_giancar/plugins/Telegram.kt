package com.el_giancar.plugins

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.row
import io.ktor.server.application.*

@OptIn(PreviewFeature::class)
suspend fun Application.configureTelegram() {
    val bot = telegramBot(System.getenv("TOKEN"))

    val rulesMsg: String = "The rules are pretty simple which you could even memorize \uD83E\uDDE0 \n\n" +
            "- Scissors cuts Paper \n" +
            "- Paper covers Rock \n" +
            "- Rock crushes  Lizard \n" +
            "- Lizard poisons Spock\n" +
            "- Spock smashes Scissors\n" +
            "- Scissors decapitates Lizard\n" +
            "- Lizard eats Paper\n" +
            "- Paper disproves Spock\n" +
            "- Spock vaporizes Rock\n" +
            "- (and as it always has) Rock crushes Scissors"


    bot.buildBehaviourWithLongPolling {
        println(getMe())

        onCommand("start") {
            reply(it, "Hi! \uD83D\uDD25")
        }

        onCommand("rules") {
            reply(it, rulesMsg)
        }

        onCommand("commands") {
            reply(
                it,
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        simpleButton("/start")
                        simpleButton("/rules")
                        simpleButton("/new_game")
                    }
                    row {
                        simpleButton("/commands")
                    }
                    row {
                        simpleButton("/about")
                        simpleButton("/help")
                    }
                }
            ) {
                +"Use the buttons to know"
            }
        }

        onUnhandledCommand {
            reply(
                it,
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        simpleButton("/commands")
                    }
                }
            ) {
                +"Use " + botCommand("commands") + " to get the available commands"
            }
        }
    }.join()
}
