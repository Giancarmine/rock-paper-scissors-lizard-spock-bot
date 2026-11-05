package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.Move
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.utils.row

internal fun commandsKeyboard(): ReplyKeyboardMarkup = replyKeyboard(
    resizeKeyboard = true,
    oneTimeKeyboard = true,
) {
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

internal fun <M : Move> movesKeyboard(moves: List<M>): ReplyKeyboardMarkup = replyKeyboard(
    resizeKeyboard = true,
    oneTimeKeyboard = true,
) {
    moves.forEach { move ->
        row {
            simpleButton(move.displayName)
        }
    }
}

internal fun unhandledCommandKeyboard(): ReplyKeyboardMarkup = replyKeyboard(
    resizeKeyboard = true,
    oneTimeKeyboard = true,
) {
    row {
        simpleButton("/commands")
    }
}
