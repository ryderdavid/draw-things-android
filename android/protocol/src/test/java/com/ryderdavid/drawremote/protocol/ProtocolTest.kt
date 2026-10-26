package com.ryderdavid.drawremote.protocol

import com.ryderdavid.drawremote.schema.GenerationConfiguration
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ProtocolTest {
    @Test fun configRoundTripPreservesDefaultsUnitsAndNestedLora() {
        val j=RemoteClient.defaults().put("model","test.ckpt").put("seed",4294967295L).put("loras",JSONArray().put(JSONObject().put("file","style.ckpt").put("weight",0.8)))
        val read=GenerationConfiguration.getRootAsGenerationConfiguration(ByteBuffer.wrap(ConfigSchema.encode(j)))
        assertEquals(8,read.startWidth());assertEquals(8,read.startHeight());assertEquals(4294967295L,read.seed());assertEquals("test.ckpt",read.model())
        assertEquals(0.7f,read.hiresFixStrength(),0.0001f);assertEquals("style.ckpt",read.loras(0)!!.file());assertEquals(0.8f,read.loras(0)!!.weight(),0.0001f)
    }
    @Test fun pixelCodecRoundTripsRgb() {
        val pixels=intArrayOf(0xff000000.toInt(),0xffffffff.toInt(),0xffff0000.toInt(),0xff00ff00.toInt(),0xff0000ff.toInt(),0xff7f7f7f.toInt())
        val bytes=TensorCodec.encode(TensorCodec.Image(3,2,pixels));val result=TensorCodec.decode(bytes)
        assertEquals(3,result.width);assertEquals(2,result.height);assertArrayEquals(pixels,result.argb)
    }
    @Test fun knownHalfValues() {
        assertEquals(1f,TensorCodec.halfToFloat(0x3c00),0f);assertEquals(-1f,TensorCodec.halfToFloat(0xbc00),0f)
        assertEquals(0x3c00,TensorCodec.floatToHalf(1f));assertEquals(0xbc00,TensorCodec.floatToHalf(-1f))
        assertEquals(Math.scalb(1f,-24),TensorCodec.halfToFloat(1),0f)
    }
    @Test fun rejectsTruncatedAndOversizedPackets() {
        val bytes=TensorCodec.encode(TensorCodec.Image(1,1,intArrayOf(-1)))
        assertThrows(IllegalArgumentException::class.java) { TensorCodec.decode(bytes.copyOf(bytes.size-1)) }
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(28,Int.MAX_VALUE)
        assertThrows(IllegalArgumentException::class.java) { TensorCodec.decode(bytes) }
    }
    @Test fun rejectsLatentsAndNonfinitePixels() {
        val bytes=TensorCodec.encode(TensorCodec.Image(1,1,intArrayOf(-1)))
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putShort(68,0x7e00.toShort())
        assertThrows(IllegalArgumentException::class.java) { TensorCodec.decode(bytes) }
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(32,16)
        assertThrows(IllegalArgumentException::class.java) { TensorCodec.decode(bytes) }
    }
    @Test fun maskUsesRankTwoBytesAndDrawThingsLabels() {
        val bytes=TensorCodec.encodeMask(2,1,byteArrayOf(0,2));val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1,b.getInt(8));assertEquals(0x1000,b.getInt(12));assertEquals(1,b.getInt(20));assertEquals(2,b.getInt(24));assertEquals(70,bytes.size)
    }
    @Test fun refusesImpossibleRequestsAndUntrustedPinSyntax() {
        assertThrows(IllegalArgumentException::class.java) { RemoteClient.validate(RemoteClient.defaults().put("model","m").put("steps",0)) }
        assertThrows(IllegalArgumentException::class.java) { RemoteClient.Endpoint("http://mac") }
        assertThrows(IllegalArgumentException::class.java) { RemoteClient.Endpoint("mac",pin="invalid") }
    }
}
