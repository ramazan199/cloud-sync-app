package com.cloud.sync.ui.mnemonic


import androidx.lifecycle.ViewModel
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class MnemonicViewModel @Inject constructor(
    private val keyRepository: ICseMasterKeyRepository,
) :
    ViewModel() {

    private val _uiState = MutableStateFlow(MnemonicUiState())
    val uiState: StateFlow<MnemonicUiState> = _uiState.asStateFlow()

    fun generateMnemonic(wordCount: Int = 12) {
        _uiState.update { it.copy(isLoading = true) }

        val strength = if (wordCount == 24) 256 else 128
        val entropy = ByteArray(strength / 8)
        SecureRandom().nextBytes(entropy)
        val newMnemonic = Mnemonics.MnemonicCode(entropy).joinToString(" ")

        _uiState.update {
            it.copy(
                mnemonic = newMnemonic,
                isLoading = false
            )
        }
    }


    fun saveMasterKeyFromMnemonic() {
        if (uiState.value.mnemonic.isNotBlank()) {
            val mnemonicCode = Mnemonics.MnemonicCode(uiState.value.mnemonic)
            val seed = mnemonicCode.toSeed()
            keyRepository.saveKey(seed)
            _uiState.update { it.copy(isKeySaved = true) }
        }
    }
}