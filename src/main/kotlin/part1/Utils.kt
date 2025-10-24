package io.xorltd.part1

import java.nio.ByteBuffer

fun ByteBuffer.toU8(): Int = this.get().toInt() and 0xFF

fun ByteBuffer.toU16LE(): Int {
    val lo = toU8()
    val hi = toU8()
    return (hi shl 8) or lo
}
fun signExtend8To16(b: Int): Int = (b shl 24) shr 24

fun formatDispSigned(signed: Int): String =
    if (signed == 0) "" else if (signed < 0) "-${-signed}" else "+$signed"

fun formatDispSigned16(u16: Int): String {
    val signed = if (u16 and 0x8000 != 0) u16 - 0x10000 else u16
    return formatDispSigned(signed)
}

val ea16 = arrayOf(
    "bx+si", "bx+di", "bp+si", "bp+di", "si", "di", "bp", "bx"
)

fun getRegister(byte: Byte, w: Boolean): String? {
    return if (w) hiReg[byte] else loReg[byte]
}

private val loReg = mapOf(
    0b000.toByte() to "al",
    0b001.toByte() to "cl",
    0b010.toByte() to "dl",
    0b011.toByte() to "bl",
    0b100.toByte() to "ah",
    0b101.toByte() to "ch",
    0b110.toByte() to "dh",
    0b111.toByte() to "bh",
)

private val hiReg = mapOf(
    0b000.toByte() to "ax",
    0b001.toByte() to "cx",
    0b010.toByte() to "dx",
    0b011.toByte() to "bx",
    0b100.toByte() to "sp",
    0b101.toByte() to "bp",
    0b110.toByte() to "si",
    0b111.toByte() to "di",
)
