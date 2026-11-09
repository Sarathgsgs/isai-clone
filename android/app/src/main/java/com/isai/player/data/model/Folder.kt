package com.isai.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int
)

