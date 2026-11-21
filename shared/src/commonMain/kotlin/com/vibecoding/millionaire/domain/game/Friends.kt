package com.vibecoding.millionaire.domain.game

import org.jetbrains.compose.resources.StringResource

import com.vibecoding.millionaire.generated.resources.Res
import com.vibecoding.millionaire.generated.resources.*
import com.vibecoding.millionaire.domain.model.AnswerOption

/** A fictional friend available for the "phone a friend" lifeline (FR-4.4). */
data class Friend(
    val nameRes: StringResource,
    val descriptionRes: StringResource,
)

enum class Confidence { HIGH, MEDIUM, LOW }

data class FriendHint(
    val friend: Friend,
    val answer: AnswerOption,
    val confidence: Confidence,
)

object Friends {
    val all: List<Friend> = listOf(
        Friend(Res.string.friend_1_name, Res.string.friend_1_desc),
        Friend(Res.string.friend_2_name, Res.string.friend_2_desc),
        Friend(Res.string.friend_3_name, Res.string.friend_3_desc),
        Friend(Res.string.friend_4_name, Res.string.friend_4_desc),
        Friend(Res.string.friend_5_name, Res.string.friend_5_desc),
    )
}
