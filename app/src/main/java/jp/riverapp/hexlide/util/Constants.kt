package jp.riverapp.hexlide.util

/**
 * iOS版 Constants.swift の完全移植。
 */
object Constants {
    object Api {
        const val BASE_URL = "https://hexlide.riverapp.jp"
    }

    object Timing {
        const val MOVE_ANIMATION_MS = 800L
        const val AI_THINK_DELAY_MS = 500L
        const val CONFETTI_DURATION_MS = 4000L
        const val POLLING_INTERVAL_MS = 1000L
        const val MAX_CONSECUTIVE_FAILURES = 5
    }

    object Game {
        const val HEX_SIZE = 38f
        const val PIECE_RADIUS = 20f
        const val PIECE_TOUCH_RADIUS = 30f
        const val BOARD_PADDING = 60f
    }

    object WebPages {
        const val HOW_TO_PLAY = "https://hexlide.riverapp.jp/app/how-to-play"
        const val PRIVACY = "https://hexlide.riverapp.jp/app/privacy"
        const val CONTACT = "https://hexlide.riverapp.jp/app/contact"
        const val HOMEPAGE = "https://riverapp.jp/apps/hexlide"
        const val WEBSITE = "https://hexlide.riverapp.jp/app"
        const val WEB_VERSION = "https://hexlide.riverapp.jp"
        const val GOOGLE_PLAY_REVIEW = "market://details?id=jp.riverapp.hexlide"
    }
}
