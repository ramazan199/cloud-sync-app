package com.cloud.sync.ui.mnemonic

data class MnemonicUiState(
    val mnemonic: String = "",
    val isKeySaved: Boolean = false,
    val isLoading: Boolean = false
)