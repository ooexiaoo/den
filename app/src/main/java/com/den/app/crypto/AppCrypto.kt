package com.den.app.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AppCrypto(private val context: Context) {

    private val prefs = context.getSharedPreferences("den_crypto", Context.MODE_PRIVATE)

    private companion object {
        const val MASTER_ALIAS = "den_master"
        const val GCM_TAG_BITS = 128
        const val IV_LEN = 12
        const val PBKDF2_ITERATIONS = 120_000
        const val KEY_LEN_BITS = 256
        const val PASS_SALT = "den::passcode::salt"
    }

    private val master: SecretKey by lazy {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(MASTER_ALIAS, null) as SecretKey?) ?: generateMaster()
    }

    private fun generateMaster(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                MASTER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    // ---- random helpers ----

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }

    fun newMediaName(displayName: String?): String {
        val ext = displayName?.substringAfterLast('.', "")?.takeIf { it.length in 1..6 && it.all(Char::isLetterOrDigit) }
        return "${UUID.randomUUID()}${if (ext != null) ".$ext" else ""}"
    }

    // ---- database passphrase ----

    fun dbPassphrase(): String {
        val wrapped = prefs.getString("db_pass_wrapped", null)
        if (wrapped != null) {
            return String(decrypt(Base64.decode(wrapped, Base64.NO_WRAP)), Charsets.UTF_8)
        }
        val pass = bytesToHex(randomBytes(32))
        val stored = Base64.encodeToString(encrypt(pass.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        prefs.edit().putString("db_pass_wrapped", stored).apply()
        return pass
    }

    // ---- master-key AEAD ----

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // AndroidKeyStore forbids caller-provided IVs (randomized encryption requirement);
        // let the keystore generate one and read it back after init.
        cipher.init(Cipher.ENCRYPT_MODE, master)
        val iv = cipher.iv
        val ct = cipher.doFinal(data)
        return iv + ct
    }

    fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = data.copyOfRange(0, IV_LEN)
        val ct = data.copyOfRange(IV_LEN, data.size)
        cipher.init(Cipher.DECRYPT_MODE, master, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    fun streamEncryptSink(out: OutputStream): OutputStream {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, master)
        val iv = cipher.iv
        out.write(iv)
        return CipherOutputStream(out, cipher)
    }

    fun streamDecryptSource(ins: InputStream): InputStream {
        val iv = ByteArray(IV_LEN)
        readFully(ins, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, master, GCMParameterSpec(GCM_TAG_BITS, iv))
        return CipherInputStream(ins, cipher)
    }

    // ---- passphrase-based AEAD (cross-device backups) ----

    fun readFully(ins: InputStream, buf: ByteArray) {
        var offset = 0
        while (offset < buf.size) {
            val read = ins.read(buf, offset, buf.size - offset)
            if (read < 0) throw IllegalStateException("Unexpected end of stream")
            offset += read
        }
    }

    private fun passphraseKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LEN_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun passphraseEncryptSink(passphrase: String, out: OutputStream): OutputStream {
        val salt = randomBytes(16)
        val key = passphraseKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = randomBytes(IV_LEN)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        out.write(salt)
        out.write(iv)
        return CipherOutputStream(out, cipher)
    }

    fun passphraseDecryptSource(passphrase: String, ins: InputStream): InputStream {
        val salt = ByteArray(16)
        readFully(ins, salt)
        val key = passphraseKey(passphrase, salt)
        val iv = ByteArray(IV_LEN)
        readFully(ins, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return CipherInputStream(ins, cipher)
    }

    fun passphraseEncryptBytes(passphrase: String, data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        passphraseEncryptSink(passphrase, out).use { it.write(data) }
        return out.toByteArray()
    }

    fun passphraseDecryptBytes(passphrase: String, data: ByteArray): ByteArray {
        val ins = passphraseDecryptSource(passphrase, data.inputStream())
        return ins.use { it.readBytes() }
    }

    // ---- passcode hashing ----

    fun hashPasscode(passcode: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashed = md.digest((PASS_SALT + passcode).toByteArray(Charsets.UTF_8))
        fun stretch(round: Int, input: ByteArray): ByteArray {
            var cur = input
            repeat(round) { cur = md.digest(cur) }
            return cur
        }
        val stretched = stretch(1000, hashed)
        return bytesToHex(stretched)
    }

    fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}