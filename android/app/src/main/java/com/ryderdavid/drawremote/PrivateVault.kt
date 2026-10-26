package com.ryderdavid.drawremote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.*
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/** Each record is one atomic authenticated envelope; no plaintext index or disk image cache. */
class PrivateVault(context:Context) {
    private val directory=File(context.noBackupFilesDir,"vault").apply { check(isDirectory || mkdirs()) }
    private val keyStore=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key():SecretKey {
        (keyStore.getKey(ALIAS,null) as? SecretKey)?.let { return it }
        check(directory.listFiles().orEmpty().none { it.extension=="vault" || it.extension=="bak" }) { "Vault key is unavailable. Existing records cannot be opened; reset must be explicit." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build()); generateKey()
        }
    }
    @Synchronized fun write(id:String,metadata:JSONObject,payload:ByteArray=byteArrayOf()) {
        require(UUID.fromString(id).toString()==id)
        require(payload.size<=32*1024*1024)
        val meta=metadata.toString().toByteArray(Charsets.UTF_8);require(meta.size<=1024*1024)
        val plaintext=ByteArrayOutputStream().use { out -> DataOutputStream(out).use { it.writeInt(meta.size);it.write(meta);it.write(payload) };out.toByteArray() }
        // Keystore only wraps a short random data key. Some older hardware-backed GCM
        // implementations fail to authenticate image-sized requests (covered on-device).
        val dataKey=ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrapping=Cipher.getInstance("AES/GCM/NoPadding");wrapping.init(Cipher.ENCRYPT_MODE,key())
        wrapping.updateAAD(("DTR2:key:"+id).toByteArray())
        val wrappedKey=wrapping.doFinal(dataKey)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(dataKey,"AES"))
        cipher.updateAAD(("DTR2:data:"+id).toByteArray())
        val encrypted=try { cipher.doFinal(plaintext) } finally { plaintext.fill(0);dataKey.fill(0) }
        val file=AtomicFile(File(directory,"$id.vault"));val output=file.startWrite()
        try { output.write(MAGIC);output.write(wrapping.iv);output.write(wrappedKey);output.write(cipher.iv);output.write(encrypted);file.finishWrite(output) }
        catch(t:Throwable) { file.failWrite(output);throw t }
    }
    @Synchronized fun read(id:String):Pair<JSONObject,ByteArray>? {
        require(UUID.fromString(id).toString()==id)
        val file=AtomicFile(File(directory,"$id.vault"))
        if(!file.baseFile.exists() && !File(file.baseFile.path+".bak").exists())return null
        val bytes=file.openRead().use { input ->
            require(input.channel.size() in 32..(34L*1024*1024)) { "Invalid vault record size" };input.readBytes()
        }
        val plain:ByteArray
        if(bytes.copyOfRange(0,4).contentEquals(byteArrayOf(68,84,82,49))) {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(4,16)))
            cipher.updateAAD(("DTR1:"+id).toByteArray());plain=cipher.doFinal(bytes,16,bytes.size-16)
        } else {
            require(bytes.size>=96 && bytes.copyOfRange(0,4).contentEquals(MAGIC)) { "Unsupported vault version" }
            val wrapping=Cipher.getInstance("AES/GCM/NoPadding");wrapping.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(4,16)))
            wrapping.updateAAD(("DTR2:key:"+id).toByteArray())
            val dataKey=wrapping.doFinal(bytes,16,48)
            try {
                require(dataKey.size==32)
                val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(dataKey,"AES"),GCMParameterSpec(128,bytes.copyOfRange(64,76)))
                cipher.updateAAD(("DTR2:data:"+id).toByteArray());plain=cipher.doFinal(bytes,76,bytes.size-76)
            } finally { dataKey.fill(0) }
        }
        try {
            DataInputStream(ByteArrayInputStream(plain)).use { input ->
                val n=input.readInt();require(n in 0..1024*1024 && n<=plain.size-4)
                val meta=ByteArray(n);input.readFully(meta)
                return JSONObject(meta.toString(Charsets.UTF_8)) to input.readBytes()
            }
        } finally { plain.fill(0) }
    }
    @Synchronized fun images():List<Pair<String,JSONObject>> = directory.listFiles().orEmpty()
        .filter { it.extension=="vault" && it.nameWithoutExtension !in SYSTEM_IDS }
        .map { file -> file.nameWithoutExtension to try { read(file.nameWithoutExtension)?.first ?: error("Missing vault record") }
            catch(e:Exception) { JSONObject().put("prompt","Unreadable encrypted record").put("unreadable",true) } }
        .sortedByDescending { it.second.optLong("created") }
    @Synchronized fun delete(id:String) { require(UUID.fromString(id).toString()==id);AtomicFile(File(directory,"$id.vault")).delete() }
    companion object {
        private const val ALIAS="draw-remote-vault-v1"
        private val MAGIC=byteArrayOf(68,84,82,50)
        const val CONNECTION="8e22d044-52cd-44fa-980d-2665cf89fb01"
        const val DRAFT="8e22d044-52cd-44fa-980d-2665cf89fb02"
        const val JOB="8e22d044-52cd-44fa-980d-2665cf89fb03"
        private val SYSTEM_IDS=setOf(CONNECTION,DRAFT,JOB)
    }
}
