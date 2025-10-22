package io.xorltd.part1

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

// bit patterns
// MOV
// MOV AL = low (only low) AH = high AX = 16bit
// 8          8
// [......DW] [MOD][REG][RM]
//            register reg/mem
// MOD - 11 = reg -> reg
// D = flips REG & RM
// W = is this 16bit or 8bit (wide)

fun decodeInstructions(binaryFile: String): String {
    val file = File(binaryFile)
    val bytes = file.readBytes()
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    val sb = StringBuilder()

    while (buffer.hasRemaining()) {
        val instruction = getInstruction(buffer)
        sb.append(instruction.asmString)
        sb.append("\n")
    }

    val toString = sb.toString()
    println(toString)
    return toString
}

private fun getInstruction(buffer: ByteBuffer): Instruction {
    val opcode = buffer.get().toInt() and 0xFF
    return when {
        // MOV - bit mask last 2 bits
        opcode and 0b11111100 == 0b10001000 -> {
            val w = opcode and 0b1
            val modrmByte = buffer.get()
            val reg = ((modrmByte.toInt() shr 3) and 0b111).toByte()
            val rm = (modrmByte.toInt() and 0b111).toByte()
            Instruction.Mov(getRegister(reg, w == 1)!!, getRegister(rm, w == 1)!!)
        }
        else -> throw IllegalArgumentException("Unknown instruction")
    }
}

private fun getRegister(byte: Byte, w: Boolean): String? {
    return if (w) highRegister[byte] else lowRegister[byte]
}


sealed interface Instruction {
    val mnemonic: String
    val asmString: String
        get() = when (this) {
            is Mov -> "$mnemonic $to, $from"
        }

    data class Mov(val from: String, val to: String) : Instruction {
        override val mnemonic: String = "mov"
    }

}

private val lowRegister = mapOf(
    0b000.toByte() to "al",
    0b001.toByte() to "cl",
    0b010.toByte() to "dl",
    0b011.toByte() to "bl",
    0b100.toByte() to "ah",
    0b101.toByte() to "ch",
    0b110.toByte() to "dh",
    0b111.toByte() to "bh",
)

private val highRegister = mapOf(
    0b000.toByte() to "ax",
    0b001.toByte() to "cx",
    0b010.toByte() to "dx",
    0b011.toByte() to "bx",
    0b100.toByte() to "sp",
    0b101.toByte() to "bp",
    0b110.toByte() to "si",
    0b111.toByte() to "di",
)
