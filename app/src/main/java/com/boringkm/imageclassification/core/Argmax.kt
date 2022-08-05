package com.boringkm.imageclassification.core

fun argmax(array: FloatArray): Pair<Int, Float> {
    var argmax = 0
    var max = array[0]
    for (i in 1 until array.size) {
        val f = array[i]
        if (f > max) {
            argmax = i
            max = f
        }
    }
    return Pair(argmax, max)
}

fun argmax(map: Map<String, Float>): Pair<String, Float> {
    var maxKey = ""
    var maxVal = -1f

    for (entry: Map.Entry<String, Float> in map.entries) {
        val f = entry.value
        if (f > maxVal) {
            maxKey = entry.key
            maxVal = f
        }
    }
    return Pair(maxKey, maxVal)
}