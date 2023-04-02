package com.el_giancar.plugins

import com.el_giancar.model.ClassicMoveType
import com.el_giancar.model.SheldonMoveType
import com.el_giancar.model.getNames
import com.el_giancar.srv.ClassicGameService
import com.el_giancar.srv.SheldonGameService
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

@OptIn(PreviewFeature::class)
suspend fun configureTelegram() {
    val bot = telegramBot("5864419054:AAFi6Nbd5qIADkhpA_mBFbsos9YrK6A_u3E")
    val classicGameSrv = ClassicGameService()
    val sheldonGameSrv = SheldonGameService()

    val rulesMsg: String = "The rules are pretty simple which you could even memorize \uD83E\uDDE0 \n\n" +
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


    bot.buildBehaviourWithLongPolling {
        println(getMe())

        onCommand("start") {
            reply(it, "Hi! Click at /commands to start playing \uD83D\uDD25")
            printCommandsMenu(it)
        }

        onCommand("rules") {
            reply(it, rulesMsg)
            printCommandsMenu(it)
        }

        onCommand("classic_game") { it ->
            val playerMove = waitText(
                SendTextMessage(
                    it.chat.id,
                    "Send me your \"choose\"",
                    replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                        row {
                            simpleButton(ClassicMoveType.ROCK.name)
                        }
                        row {
                            simpleButton(ClassicMoveType.PAPER.name)
                        }
                        row {
                            simpleButton(ClassicMoveType.SCISSORS.name)
                        }
                    }
                )
            ).first().text.takeIf { it in getNames<SheldonMoveType>() }

            val result = classicGameSrv.play(playerMove!!)
            reply(it, result)

            printCommandsMenu(it)
        }

        onCommand("sheldon_game") { it ->
            val playerMove = waitText(
                SendTextMessage(
                    it.chat.id,
                    "Send me your \"choose\"",
                    replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                        row {
                            simpleButton(SheldonMoveType.ROCK.name)
                        }
                        row {
                            simpleButton(SheldonMoveType.PAPER.name)
                        }
                        row {
                            simpleButton(SheldonMoveType.SCISSORS.name)
                        }
                        row {
                            simpleButton(SheldonMoveType.LIZARD.name)
                        }
                        row {
                            simpleButton(SheldonMoveType.SPOCK.name)
                        }
                    }
                )
            ).first().text.takeIf { it in getNames<SheldonMoveType>() }

            val result = sheldonGameSrv.play(playerMove!!)
            reply(it, result)

            printCommandsMenu(it)
        }

        onCommand("commands") {
            printCommandsMenu(it)
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

private suspend fun BehaviourContext.printCommandsMenu(it: CommonMessage<TextContent>) {
    reply(
        it,
        replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
            row {
                simpleButton("/classic_game")
                simpleButton("/sheldon_game")
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
