package com.ryderdavid.drawremote.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Draw Things' 68-byte ccv tensor header. This is a pixel codec, never inference. */
object TensorCodec {
    const val MAX_PIXELS = 4 * 1024 * 1024
    const val MAX_BYTES = 32 * 1024 * 1024
    data class Image(val width: Int, val height: Int, val argb: IntArray)
    private fun buffer(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    fun decode(bytes: ByteArray, decompress:((ByteArray,Int)->FloatArray)?=null): Image {
        require(bytes.size in 68..MAX_BYTES) { "Invalid image packet size" }
        val b = buffer(bytes)
        val compression = b.getInt(0)
        require(compression == 0 || compression == 1012247) { "Unsupported tensor compression" }
        require(b.getInt(8) == 2 && b.getInt(20) == 1) { "Expected one NHWC image" }
        val type = b.getInt(12)
        require(type == 0x20000 || type == 0x4000) { "Unsupported pixel type" }
        val height = b.getInt(24); val width = b.getInt(28); val channels = b.getInt(32)
        require(width in 1..4096 && height in 1..4096 && width.toLong() * height <= MAX_PIXELS) { "Image exceeds client memory limit" }
        require(channels == 3 || channels == 4) { "This packet is a latent, not a decoded image" }
        require((9..16).all { b.getInt(it * 4) == 0 }) { "Unsupported tensor dimensions" }
        val stride = if (type == 0x20000) 2 else 4
        if(compression==0) require(bytes.size.toLong() == 68L + width.toLong() * height * channels * stride) { "Truncated or oversized image" }
        val floats=if(compression==1012247) { requireNotNull(decompress) { "fpzip codec is unavailable" }.invoke(bytes.copyOfRange(68,bytes.size),width*height*channels).also { require(it.size==width*height*channels) } } else null
        b.position(68)
        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            var pixel = -0x1000000
            repeat(channels) { c ->
                val f = floats?.get(i*channels+c) ?: if (stride == 2) halfToFloat(b.short.toInt() and 0xffff) else b.float
                require(f.isFinite()) { "Mac returned non-finite pixels" }
                val v = ((f + 1f) * 127.5f).roundToInt().coerceIn(0,255)
                if (c < 3) pixel = pixel or (v shl (16 - 8*c))
            }
            pixels[i] = pixel
        }
        return Image(width, height, pixels)
    }
    fun encode(image: Image): ByteArray {
        require(image.width > 0 && image.height > 0 && image.width.toLong() * image.height <= MAX_PIXELS)
        require(image.argb.size == image.width * image.height)
        val b = ByteBuffer.allocate(68 + image.argb.size * 6).order(ByteOrder.LITTLE_ENDIAN)
        intArrayOf(0,1,2,0x20000,0,1,image.height,image.width,3).forEachIndexed { i,v -> b.putInt(i*4,v) }
        b.position(68)
        for (p in image.argb) repeat(3) { c -> b.putShort(floatToHalf(((p ushr (16-c*8)) and 255) / 127.5f - 1f).toShort()) }
        return b.array()
    }
    fun encodeMask(width: Int, height: Int, mask: ByteArray): ByteArray {
        require(width > 0 && height > 0 && width.toLong()*height <= MAX_PIXELS && mask.size == width*height)
        require(mask.all { it.toInt() in 0..3 })
        val b=ByteBuffer.allocate(68+mask.size).order(ByteOrder.LITTLE_ENDIAN)
        intArrayOf(0,1,1,0x1000,0,height,width,0,0).forEachIndexed { i,v -> b.putInt(i*4,v) }
        b.position(68); b.put(mask); return b.array()
    }
    fun halfToFloat(h: Int): Float {
        val sign = if (h and 0x8000 != 0) -1 else 1
        val e = (h ushr 10) and 31; val m = h and 1023
        return when(e) {
            0 -> sign * Math.scalb(m.toFloat(), -24)
            31 -> if(m==0) sign * Float.POSITIVE_INFINITY else Float.NaN
            else -> sign * Math.scalb((1024+m).toFloat(), e-25)
        }
    }
    fun floatToHalf(f: Float): Int {
        val bits=f.toRawBits(); val sign=(bits ushr 16) and 0x8000
        val exp=((bits ushr 23) and 255)-127+15; val mant=bits and 0x7fffff
        if(exp<=0) { if(exp < -10) return sign; val m=mant or 0x800000; return sign or ((m + (1 shl (13-exp))) ushr (14-exp)) }
        if(exp>=31) return sign or 0x7c00 or if(mant!=0) 0x200 else 0
        return sign or ((exp shl 10) + ((mant+0x1000) ushr 13))
    }
}
