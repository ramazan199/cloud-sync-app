package com.cloud.sync.ui.mnemonic

data class MnemonicUiState(
    val mnemonic: String = "",
    val masterKey: ByteArray? = null,
    val isLoading: Boolean = false
)