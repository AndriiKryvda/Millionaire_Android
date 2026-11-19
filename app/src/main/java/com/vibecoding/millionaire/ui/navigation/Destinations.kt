package com.vibecoding.millionaire.ui.navigation

import kotlinx.serialization.Serializable

object NavArgs {
    const val PLAYER_ID = "playerId"
    const val RESULT_ID = "resultId"
}

@Serializable
object SplashRoute

@Serializable
object PlayersRoute

@Serializable
object CreatePlayerRoute

@Serializable
data class EditPlayerRoute(val playerId: Long)

@Serializable
data class MenuRoute(val playerId: Long)

@Serializable
data class GameRoute(val playerId: Long)

@Serializable
data class ResultRoute(val resultId: Long)

@Serializable
object RecordsRoute

@Serializable
object RulesRoute

@Serializable
data class SettingsRoute(val playerId: Long)