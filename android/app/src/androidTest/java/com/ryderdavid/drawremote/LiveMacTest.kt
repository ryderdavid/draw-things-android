package com.ryderdavid.drawremote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.ryderdavid.drawremote.protocol.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in real-server tests. No default endpoint, no automatic generation during ordinary test runs. */
@RunWith(AndroidJUnit4::class)
class LiveMacTest {
    @Test fun realMacRoundTrip() {
        val args=InstrumentationRegistry.getArguments()
        val host=args.getString("macHost");assumeTrue("Set macHost to opt in",!host.isNullOrBlank())
        val endpoint=RemoteClient.Endpoint(host!!,args.getString("macPort")?.toInt()?:7859,true,pin=args.getString("macPin")?:"")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        RemoteClient(endpoint,Fpzip::decode).use { client ->
            val inventory=client.connect()
            report("inventory",inventory.models.joinToString("\n") { "${it.name}: ${it.file}" })
            val model=args.getString("macModel")?:return
            assertTrue(inventory.models.any { it.file==model })
            val cfg=RemoteClient.defaults().put("model",model).put("start_width",8).put("start_height",8).put("steps",4).put("guidance_scale",1.0).put("shift",3.0)
            val latch=CountDownLatch(1);var failure:Throwable?=null;var result:List<TensorCodec.Image>?=null
            val call=client.generate(cfg,"A small red ceramic cube on a pale gray tabletop, soft studio light", "",null,null,{ report("progress",it) },{ images,error -> result=images;failure=error;latch.countDown() })
            if(!latch.await(10,TimeUnit.MINUTES)) { call.cancel("Test timed out",null);fail("Mac generation timed out") }
            failure?.let { throw AssertionError("Mac generation failed",it) }
            val image=result!!.first();assertEquals(512,image.width);assertEquals(512,image.height)
            assertTrue("Expected nonuniform image",image.argb.toSet().size>100)
            val bitmap=Bitmap.createBitmap(image.argb,image.width,image.height,Bitmap.Config.ARGB_8888)
            val png=WorkspaceModel.png(bitmap);val id=UUID.randomUUID().toString()
            val vault=PrivateVault(context)
            vault.write(id,JSONObject().put("prompt","A small red ceramic cube on a pale gray tabletop, soft studio light").put("negative","").put("config",cfg).put("created",System.currentTimeMillis()).put("width",image.width).put("height",image.height),png)
            val reopened=PrivateVault(context).read(id)!!;assertArrayEquals(png,reopened.second)
            val decoded=BitmapFactory.decodeByteArray(reopened.second,0,reopened.second.size);assertEquals(512,decoded.width)
            vault.write(PrivateVault.CONNECTION,endpoint.json())
            vault.write(PrivateVault.DRAFT,JSONObject().put("prompt","A small red ceramic cube on a pale gray tabletop, soft studio light").put("negative","").put("config",cfg))
            report("verified","Mac-generated ${image.width}x${image.height}; encrypted record $id reopened; ${png.size} PNG bytes")
        }
    }
    private fun report(key:String,value:String) { InstrumentationRegistry.getInstrumentation().sendStatus(0,Bundle().apply { putString(key,value) }) }
}
