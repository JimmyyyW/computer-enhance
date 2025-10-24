package part1

import io.xorltd.part1.decodeInstructions
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

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

    @Test
    fun `should match the original assembly with a variety of instructions`() {
        forFile("listing_39_more_movs", ::binaryMatchesExpected)
    }

    private fun binaryMatchesExpected(
        binaryFile: File,
    ) {
        val outputAsm = File.createTempFile("output", ".asm")
        val p = Paths.get(outputAsm.absolutePath)
        Files.newBufferedWriter(p, Charsets.UTF_8).use { writer ->
            writer.newLine()
            writer.write(decodeInstructions(binaryFile))
        }

        val asmPath = outputAsm.absolutePath
        runNasm(asmPath) { outputBin ->
            val originalBytes = binaryFile.readBytes()
            val outputBytes = outputBin.readBytes()
            assertTrue(originalBytes.contentEquals(outputBytes))
        }
    }

    private fun forFile(fileName: String, block: (File) -> Unit) {
        val binaryFile = File("$BASE_DIR/$fileName")
        block(binaryFile)
    }

    fun runNasm(asmPath: String, block: (File) -> Unit) {
        val outPath = asmPath + "-out.bin"
        val pb = ProcessBuilder("nasm", "-f", "bin", asmPath, "-o", outPath)
            .redirectErrorStream(true)
        val proc = pb.start()
        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()
        println("NASM exit code: $exit")
        println("NASM output:\n$output")
        block(File(outPath))
    }

    companion object {
        private const val BASE_DIR: String = "src/test/resources/instructions"
    }

}
