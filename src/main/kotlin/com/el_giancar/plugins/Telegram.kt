package com.el_giancar.plugins

import com.el_giancar.srv.ClassicGameService
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommandWithArgs
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.row
import io.ktor.server.application.*
import kotlinx.coroutines.flow.first

@OptIn(PreviewFeature::class)
suspend fun Application.configureTelegram() {
    val bot = telegramBot(System.getenv("TOKEN"))
    val classicGameSrv = ClassicGameService()

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

        onCommand("new_game") {
            reply(it, "To play a new game... You have to wait that the developer implement it \uD83D\uDE05")
        }

        onCommand("classic_game") {
            val playerMove = waitText (
                SendTextMessage(
                    it.chat.id,
                    "Send me your name or choose \"nope\"",
                    replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                        row {
                            simpleButton("rock")
                        }
                        row {
                            simpleButton("paper")
                        }
                        row {
                            simpleButton("scissors")
                        }
                    }
                )
            ).first().text.takeIf { it in arrayOf("rock", "paper", "scissors") }

            val result = classicGameSrv.play(playerMove!!)
            reply(it, result)
        }


        onCommand("commands") {
            reply(
                it,
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        simpleButton("/classic_game")
                        simpleButton("/new_game")
                        simpleButton("/rules")
                    }
                    row {
                        simpleButton("/help")
                        simpleButton("/commands")
                        simpleButton("/about")
                    }
                }
            ) {
                +"Use the buttons to start"
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
