package io.xorltd

import io.xorltd.part1.decodeInstructions
import java.io.File

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please provide a binary file path as a single argument.")
    }

    val binPath = args[0]
    println(decodeInstructions(File(binPath)))
    return
}