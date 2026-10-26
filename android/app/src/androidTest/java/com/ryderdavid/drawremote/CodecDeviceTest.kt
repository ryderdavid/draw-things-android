package com.ryderdavid.drawremote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.assertThrows
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CodecDeviceTest {
    @Test fun malformedCompressedDataFailsWithoutNativeCrash() {
        for (length in listOf(0, 15, 16, 128, 4096)) {
            assertThrows(IllegalArgumentException::class.java) { Fpzip.decode(ByteArray(length), 12) }
        }
        assertThrows(IllegalArgumentException::class.java) { Fpzip.decode(ByteArray(64), 0) }
        assertThrows(IllegalArgumentException::class.java) { Fpzip.decode(ByteArray(64), Int.MAX_VALUE) }
    }
}
