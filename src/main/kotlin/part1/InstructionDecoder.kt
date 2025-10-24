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

fun decodeInstructions(binaryFile: File): String {
    val bytes = binaryFile.readBytes()
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    val sb = StringBuilder()
    sb.append("bits 16\n\n")
    var instructionIdx = 0

    runCatching {
        while (buffer.hasRemaining()) {
            val instruction = getInstruction(buffer)
            sb.append(instruction.asmString)
            sb.append("\n")
            instructionIdx++
        }

        sb.toString()
    }.fold(
        onSuccess = { return it },
        onFailure = {
            throw IllegalArgumentException("Failed to decode instructions at instruction index $instructionIdx", it)
        }
    )
}

private fun getInstruction(buffer: ByteBuffer): Instruction {
    val opcode = buffer.get().toInt() and 0xFF
    return when {
        // MOV - bit mask last 2 bits
        opcode and 0b11111100 == 0b10001000 -> {
            val d = (opcode shr 1) and 0b1
            val w = opcode and 0b1
            val modrmByte = buffer.get()
            val reg = ((modrmByte.toInt() shr 3) and 0b111).toByte()
            val rm = (modrmByte.toInt() and 0b111).toByte()
            val (rmOperand, _) = decodeRmOperand(buffer, (modrmByte.toInt() shr 6) and 0b11, rm.toInt(), w.isOne())
            if (d.isOne()) {
                Instruction.Mov(
                    rmOperand,
                    getRegister(reg, w.isOne())!!
                )
            } else {
                Instruction.Mov(
                    getRegister(reg, w.isOne())!!,
                    rmOperand
                )
            }
        }

        // MOV immediate to register
        opcode and 0b11110000 == 0b10110000 -> {
            val w = (opcode shr 3) and 0x1
            val reg = (opcode and 0b000000111).toByte()
            val w1 = w.isOne()
            val value = if (w1) {
                val low = buffer.get().toInt() and 0xFF
                val high = buffer.get().toInt() and 0xFF
                (high shl 8) or low
            } else {
                buffer.get().toInt() and 0xFF
            }
            val regName = getRegister(reg, w1)!!

            Instruction.Immediate(regName, value and if (w1) 0xFFFF else 0xFF)
        }

        else -> {
            println("opcode: $opcode (${opcode.toString(2).padStart(8, '0')})")
            throw IllegalArgumentException("Unknown instruction")
        }
    }
}


sealed interface Instruction {
    val mnemonic: String
    val asmString: String
        get() = when (this) {
            is Mov -> "$mnemonic $to, $from"
            is Immediate -> "$mnemonic $to, ${formatImmediate(to, value)}"
        }

    data class Mov(val from: String, val to: String) : Instruction {
        override val mnemonic: String = "mov"
    }

    data class Immediate(val to: String, val value: Int) : Instruction {
        override val mnemonic: String = "mov"

        fun formatImmediate(regName: String, value: Int): String {
            val wordRegs = setOf("ax", "bx", "cx", "dx", "sp", "bp", "si", "di")
            val v = if (regName.lowercase() in wordRegs) value and 0xFFFF else value and 0xFF
            return v.toString()
        }
    }

}


private fun Int.isOne() : Boolean = this == 1

private fun decodeRmOperand(buffer: ByteBuffer, mod: Int, rm: Int, w: Boolean): Pair<String, Boolean> {
    return if (mod == 3) {
        // register-direct
        val regName = getRegister(rm.toByte(), w)!!
        regName to false
    } else {
        when (mod) {
            0 -> {
                if (rm == 6) {
                    // direct 16-bit address (disp16)
                    val disp = buffer.toU16LE()
                    "[$disp]" to true
                } else {
                    val base = ea16[rm]
                    "[$base]" to true
                }
            }
            1 -> {
                // 8-bit signed displacement
                val b = buffer.toU8()
                val s = signExtend8To16(b)
                val base = ea16[rm]
                "[$base${formatDispSigned(s)}]" to true
            }
            2 -> {
                // 16-bit displacement (low then high)
                val disp = buffer.toU16LE()
                val base = ea16[rm]
                "[$base${formatDispSigned16(disp)}]" to true
            }
            else -> throw IllegalArgumentException("invalid MOD")
        }
    }
}
