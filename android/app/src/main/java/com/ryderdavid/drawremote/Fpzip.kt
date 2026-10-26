package com.ryderdavid.drawremote

/** Lossless floating-point image decompression only. Contains no learned weights. */
object Fpzip {
    init { System.loadLibrary("draw_remote_codec") }
    private external fun decodeNative(bytes:ByteArray,expected:Int):FloatArray
    fun decode(bytes:ByteArray,expected:Int):FloatArray = decodeNative(bytes,expected)
}
