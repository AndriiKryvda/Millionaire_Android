package com.vibecoding.millionaire.domain.game

import androidx.annotation.StringRes
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.domain.model.AnswerOption

/** A fictional friend available for the "phone a friend" lifeline (FR-4.4). */
data class Friend(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
)

enum class Confidence { HIGH, MEDIUM, LOW }

data class FriendHint(
    val friend: Friend,
    val answer: AnswerOption,
    val confidence: Confidence,
)

object Friends {
    val all: List<Friend> = listOf(
        Friend(R.string.friend_1_name, R.string.friend_1_desc),
        Friend(R.string.friend_2_name, R.string.friend_2_desc),
        Friend(R.string.friend_3_name, R.string.friend_3_desc),
        Friend(R.string.friend_4_name, R.string.friend_4_desc),
        Friend(R.string.friend_5_name, R.string.friend_5_desc),
    )
}