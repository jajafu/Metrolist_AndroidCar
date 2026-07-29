/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

internal object MediaControllerCommandPolicy {
    fun canUseCustomCommands(isTrustedController: Boolean): Boolean = isTrustedController
}
