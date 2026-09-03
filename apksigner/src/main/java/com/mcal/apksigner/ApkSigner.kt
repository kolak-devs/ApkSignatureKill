package com.mcal.apksigner

import com.android.apksig.ApkSigner
import com.android.apksigner.ApkSignerTool
import com.mcal.apksigner.utils.JKS
import com.mcal.common.data.ReactivePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Provider
import java.security.Security
import java.security.cert.X509Certificate

class ApkSigner {
    fun sign(inputPath: String, outputPath: String, pk8Path: String, x509Path: String): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = signApk(inputPath, outputPath, pk8Path, x509Path)
        }
        return fallback
    }

    private suspend fun signApk(inputPath: String, outputPath: String, pk8Path: String, x509Path: String): Boolean =
        withContext(Dispatchers.IO) {
            val args = mutableListOf(
                "sign",
                "--in",
                inputPath,
                "--out",
                outputPath,
                "--key",
                pk8Path,
                "--cert",
                x509Path
            )
            try {
                ApkSignerTool.main(args.toTypedArray())
                return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }

    fun sign(
        inputPath: File,
        outputPath: File,
        keyPath: File,
        certPass: String,
        certAlias: String,
        keyPass: String
    ): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = signApk(
                inputPath,
                outputPath,
                keyPath,
                certPass,
                certAlias,
                keyPass
            )
        }
        return fallback
    }

    private suspend fun signApk(
        inputPath: File,
        outputPath: File,
        keyPath: File,
        certPass: String,
        certAlias: String,
        keyPass: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            keyPath.takeIf { it.exists() }?.let { keyPath ->
                val keystore = loadKeyStore(FileInputStream(keyPath), certPass.toCharArray())
                val signerConfig = ApkSigner.SignerConfig.Builder(
                    "CERT",
                    keystore.getKey(certAlias, keyPass.toCharArray()) as PrivateKey,
                    listOf(keystore.getCertificate(certAlias) as X509Certificate)
                ).build()
                ApkSigner.Builder(listOf(signerConfig)).apply {
                    setInputApk(inputPath)
                    setOutputApk(outputPath)
                    when (ReactivePreferences.getSigningVersion()) {
                        1 -> setV1SigningEnabled(true)
                        2 -> {
                            setV1SigningEnabled(true)
                            setV2SigningEnabled(true)
                        }
                        3 -> {
                            setV1SigningEnabled(true)
                            setV2SigningEnabled(true)
                            setV3SigningEnabled(true)
                        }
                        4 -> {
                            setV1SigningEnabled(true)
                            setV2SigningEnabled(true)
                            setV3SigningEnabled(true)
                            setV4SigningEnabled(true)
                        }
                    }
                }.build().sign()
                return@withContext true
            } ?: run {
                throw FileNotFoundException("KeyStore file not found.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    @Throws(Exception::class)
    private fun loadKeyStore(keystorePath: FileInputStream, password: CharArray): KeyStore {
        var keyStore: KeyStore
        try {
            keyStore = KeyStore.getInstance("jks")
            keyStore.load(keystorePath, password)
        } catch (e: Exception) {
            val provider = BouncyCastleProvider()
            Security.addProvider(provider)
            try {
                keyStore = JksKeyStore(provider)
                keyStore.load(keystorePath, password)
            } catch (e: Exception) {
                try {
                    keyStore = KeyStore.getInstance("bks", provider)
                    keyStore.load(keystorePath, password)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to load keystore: " + e.message)
                }
            }
        } finally {
            keystorePath.close()
        }
        return keyStore
    }
}

class JksKeyStore(provider: Provider) : KeyStore(JKS(), provider, "jks")