package part1

import io.xorltd.part1.decodeInstructions
import java.io.File
import kotlin.test.Test

class InstructionDecoderTest {

    /**
     * basic test harness.
     *
     * given a binary file, decode it to assembly instructions and then recompile to binary
     * to ensure it matches the original binary.
     */
    @Test
    fun `should match the original assembly`() {
        forFile("simple-reg-to-reg", ::binaryMatchesExpected)
    }

    @Test
    fun `should match the original assembly with mixed instructions`() {
        forFile("many-move", ::binaryMatchesExpected)
    }

    @Test
    fun `should match the original assembly with immediate instructions`() {
        forFile("simple-immediate-to-reg", ::binaryMatchesExpected)
    }

//    @Test
//    fun `should match the original assembly with a variety of instructions`() {
//        forFile("listing_39_more_movs", ::binaryMatchesExpected)
//    }

    private fun binaryMatchesExpected(
        binaryFile: File,
    ): Boolean {
        val outputBinary = File.createTempFile("output", ".bin")

        // the function under test
        outputBinary.writeText(decodeInstructions(binaryFile))

        val processNasm = ProcessBuilder(
            "nasm",
            "-f",
            "bin",
            binaryFile.absolutePath,
            "-o",
            outputBinary.absolutePath
        ).start()

        if (processNasm.waitFor() == 0) {
            val originalBytes = binaryFile.readBytes()
            val outputBytes = outputBinary.readBytes()
            return originalBytes.contentEquals(outputBytes)
        } else {
            val errorStream = processNasm.errorStream.bufferedReader().readText()
            throw RuntimeException("NASM failed: $errorStream")
        }

    }

    private fun forFile(fileName: String, block: (File) -> Unit) {
        val binaryFile = File("$BASE_DIR/$fileName")
        block(binaryFile)
    }

    companion object {
        private const val BASE_DIR: String = "src/test/resources/instructions"
    }

}