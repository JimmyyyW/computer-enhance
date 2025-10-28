package io.xorltd

import io.xorltd.part1.decodeInstructions
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please provide a binary file path as a single argument.")
        exitProcess(1)
    }

    val binPath = args[0]
    println(decodeInstructions(File(binPath)))
    return
}