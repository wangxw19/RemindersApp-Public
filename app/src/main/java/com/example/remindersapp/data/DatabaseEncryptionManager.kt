package com.example.remindersapp.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseEncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "db_secure_prefs"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32 // 32 bytes = 256 bits
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 获取数据库密码。如果不存在，则生成一个新的、随机的、加密安全的密码，
     * 并将其存储在 EncryptedSharedPreferences 中。
     * @return 用于数据库加密的密码（字节数组形式）。
     */
    fun getPassphrase(): ByteArray {
        var passphraseHex = encryptedSharedPreferences.getString(KEY_DB_PASSPHRASE, null)

        if (passphraseHex == null) {
            val newPassphrase = generateRandomPassphrase()
            passphraseHex = newPassphrase.toHexString()
            encryptedSharedPreferences.edit()
                .putString(KEY_DB_PASSPHRASE, passphraseHex)
                .apply()
            return newPassphrase
        }

        return passphraseHex.fromHexToByteArray()
    }

    private fun generateRandomPassphrase(): ByteArray {
        val random = SecureRandom()
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        random.nextBytes(passphrase)
        return passphrase
    }

    // --- Helper extension functions for hex conversion ---
    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    private fun String.fromHexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}