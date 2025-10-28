package io.xorltd.part1

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

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
            throw IllegalArgumentException(
                "Failed to decode instructions at instruction index $instructionIdx",
                it
            )
        }
    )
}

private fun getInstruction(buffer: ByteBuffer): Instruction {
    val opcode = buffer.toU8()
    return when {
        // MOV - bit mask last 2 bits
        opcode and 0b11111100 == 0b10001000 -> {
            val d = (opcode shr 1) and 0b1
            val w = opcode and 0b1
            val modrmByte = buffer.get()
            val reg = ((modrmByte.toInt() shr 3) and 0b111).toByte()
            val rm = (modrmByte.toInt() and 0b111).toByte()
            val (rmOperand, _) = decodeRmOperand(
                buffer,
                (modrmByte.toInt() shr 6) and 0b11,
                rm.toInt(),
                w.isOne()
            )
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
            val value = if (w1) buffer.toU16LE() else buffer.toU8()
            val regName = getRegister(reg, w1)!!

            Instruction.Immediate(regName, value and if (w1) 0xFFFF else 0xFF)
        }

//        opcode and 0b11111100 == 0b00000000 -> {
//            // reg/memory with register to either
//            val modrmByte = buffer.get()
//            val reg = ((modrmByte.toInt() shr 3) and 0b111).toByte()
//            val rm = (modrmByte.toInt() and 0b111).toByte()
//
//            Instruction.Mov()
//        }

        opcode and 0b11111100 == 0b10000000 -> {
            // immediate to reg/memory
            val modrmByte = buffer.get()
            val reg = ((modrmByte.toInt() shr 3) and 0b111).toByte()
            val rm = (modrmByte.toInt() and 0b111).toByte()
            val w = opcode and 0b1
            val d = (opcode shr 1) and 0b1
            val (rmOperand, _) = decodeRmOperand(
                buffer,
                (modrmByte.toInt() shr 6) and 0b11,
                rm.toInt(),
                w.isOne()
            )
            when (reg.toInt()) {
                // ADD
                0b000 -> {
                    val immValue: Int = when (opcode and 0xFF) {
                        0x81 -> buffer.toU16LE()
                        0x83 -> signExtend8To16(buffer.toU8()) and 0xFFFF
                        else -> buffer.toU8()
                    }
                    return Instruction.Add(rmOperand, "$immValue", d)
                }

                0b101 -> {
                    val value = if (w.isOne()) buffer.toU16LE() else buffer.toU8()
                    return Instruction.Sub(rmOperand, value and if (w.isOne()) 0xFFFF else 0xFF)
                }

                else -> {
                    throw IllegalArgumentException("Unknown instruction for opcode 0b10000000 with reg ${reg.toInt()}")
                }
            }

        }

        opcode and 0b11111100 == 0b00000000 -> {
            val d = (opcode shr 1) and 0b1
            val w = opcode and 0b1
            val modrmByte = buffer.get()
            val reg = ((modrmByte.toInt() shr 3) and 0b111).toByte()
            val rm = (modrmByte.toInt() and 0b111).toByte()
            val (rmOperand, _) = decodeRmOperand(
                buffer,
                (modrmByte.toInt() shr 6) and 0b11,
                rm.toInt(),
                w.isOne()
            )
            if (d.isOne()) {
                Instruction.Add(
                    rmOperand,
                    getRegister(reg, w.isOne())!!
                )
            } else {
                Instruction.Add(
                    getRegister(reg, w.isOne())!!,
                    rmOperand
                )
            }
        }

//        opcode and 0b11111110 == 0b10000100 -> {
//            // immediate to accumulator
//            val modrmByte = buffer.get()
//            Instruction.Mov()
//        }

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
            is Sub -> "$mnemonic $to, $from"
            is Add -> when (this.d) {
                0b0 -> "$mnemonic $to, $from"
                0b1 -> "$mnemonic $from, $to"
                else -> "you done goofed!"
            }

            is Cmp -> "$mnemonic $to, $from"
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

    data class Sub(
        val from: String,
        val to: Int,
    ) : Instruction {
        override val mnemonic: String = "sub"

    }

    data class Add(
        val from: String,
        val to: String,
        val d: Int? = 0b0,
    ) : Instruction {
        override val mnemonic: String = "add"
    }

    data class Cmp(
        val from: String,
        val to: Int,
    ) : Instruction {
        override val mnemonic: String = "cmp"
    }
}

private fun Int.isOne(): Boolean = this == 1

private fun decodeRmOperand(
    buffer: ByteBuffer,
    mod: Int,
    rm: Int,
    w: Boolean,
): Pair<String, Boolean> {
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
