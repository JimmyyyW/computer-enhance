package part1

import io.xorltd.part1.decodeInstructions
import java.io.File
import kotlin.test.Test

class InstructionDecoderTest {

    @Test
    fun `should match the original assembly`() {
        val file = File("src/test/resources/instructions/simple-reg-to-reg")
        val expectedInstructionsFileLines =  getInstructionsFromFile("src/test/resources/instructions/simple-reg-to-reg.asm")

        val decodedInstructions = decodeInstructions(file.path)

        val result = decodedInstructions.lines()

        assert(result.containsAll(expectedInstructionsFileLines)) {
            "Decoded instructions do not match expected instructions.\n" +
                    "Expected: $expectedInstructionsFileLines\n" +
                    "Got: $result"
        }
    }

    @Test
    fun `should match the original assembly with mixed instructions`() {
        val file = File("src/test/resources/instructions/many-move")
        val expectedInstructionsFileLines =  getInstructionsFromFile("src/test/resources/instructions/many-move.asm")

        val decodedInstructions = decodeInstructions(file.path)

        val result = decodedInstructions.lines()

        assert(result.containsAll(expectedInstructionsFileLines)) {
            "Decoded instructions do not match expected instructions.\n" +
                    "Expected: $expectedInstructionsFileLines\n" +
                    "Got: $result"
        }
    }

    private fun getInstructionsFromFile(fileName: String): List<String> {
        val file = File(fileName)
        return file.readLines().map { it.trim() }
            .filter { !it.contains("bits") }
            .filter { it.isNotEmpty() }
    }

}