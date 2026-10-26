package com.ryderdavid.drawremote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.content.pm.ApplicationInfo
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class VaultDeviceTest {
    @Test fun encryptedRecordSurvivesReopenAndRejectsTampering() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val id=UUID.randomUUID().toString();val vault=PrivateVault(context)
        val marker="private-prompt-"+UUID.randomUUID().toString();val payload="PRIVATE-IMAGE-".repeat(100).toByteArray()
        try {
            vault.write(id,JSONObject().put("prompt",marker),payload)
            val file=File(context.noBackupFilesDir,"vault/$id.vault")
            val cipher=file.readBytes()
            assertFalse(cipher.toString(Charsets.ISO_8859_1).contains(marker))
            assertFalse(cipher.toString(Charsets.ISO_8859_1).contains("PRIVATE-IMAGE"))
            val reopened=PrivateVault(context).read(id)!!
            assertEquals(marker,reopened.first.getString("prompt"));assertArrayEquals(payload,reopened.second)
            vault.write(id,JSONObject().put("prompt",marker),payload)
            assertFalse(cipher.contentEquals(file.readBytes()))
            val tampered=file.readBytes();tampered[tampered.lastIndex]=(tampered.last().toInt() xor 1).toByte();file.writeBytes(tampered)
            assertThrows(Exception::class.java) { vault.read(id) }
        } finally { vault.delete(id) }
    }
    @Test fun largeEncryptedImagesReopen() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val vault=PrivateVault(context)
        for(size in listOf(65535,65536,65537,262144,1048576)) {
            val id=UUID.randomUUID().toString()
            try {
                val payload=ByteArray(size);java.util.Random(42).nextBytes(payload)
                vault.write(id,JSONObject().put("size",size),payload)
                assertArrayEquals("Payload size $size",payload,PrivateVault(context).read(id)!!.second)
            } finally { vault.delete(id) }
        }
    }
    @Test fun noBackupOrMediaPermissions() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(0,context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        val permissions=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.any { it.contains("STORAGE")||it.contains("READ_MEDIA") })
    }
    @Test fun atomicFileRecoversInterruptedReplacement() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val id=UUID.randomUUID().toString();val vault=PrivateVault(context)
        try {
            vault.write(id,JSONObject().put("state","original"))
            val file=File(context.noBackupFilesDir,"vault/$id.vault");val atomic=android.util.AtomicFile(file)
            atomic.startWrite().use { it.write(byteArrayOf(1,2,3)) } // process dies without finishWrite
            assertEquals("original",PrivateVault(context).read(id)!!.first.getString("state"))
        } finally { vault.delete(id) }
    }
}
