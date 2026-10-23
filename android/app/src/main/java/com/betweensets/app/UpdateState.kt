package com.betweensets.app

import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Downloading(val version: Int, val percent: Int) : UpdateState
    /** Downloaded and checked. [needsPermission]: "Install unknown apps" is still off for this app. */
    data class Ready(val version: Int, val file: File, val needsPermission: Boolean) : UpdateState
    data class Installing(val version: Int) : UpdateState
    data class Failed(val reason: String) : UpdateState
}
