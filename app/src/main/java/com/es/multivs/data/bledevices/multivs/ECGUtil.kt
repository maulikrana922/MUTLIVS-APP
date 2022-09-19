package com.es.multivs.data.bledevices.multivs

import android.content.Context
import android.graphics.Point
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Created by Marko on 10/20/2021.
 * Etrog Systems LTD.
 */
class ECGUtil {
    companion object {
        fun isBadDataInEcg(
            sampleRate: Int, samples: List<Int>, upperLimit: Int, lowerLimit: Int
        ): Boolean {
            val a: Boolean = isSamplesOutOfRange(
                samples.subList(samples.size - sampleRate * 2, samples.size),
                upperLimit,
                lowerLimit
            ) //check last 2 seconds
            val b: Boolean = doesAverageChangeTooMuch(sampleRate, samples)
            return a || b
        }

        fun isSamplesOutOfRange(samples: List<Int>, upperLimit: Int, lowerLimit: Int): Boolean {
            val minMaxPoint: Point =
                getMinMaxIntoPoint(samples)
            return minMaxPoint.x < lowerLimit || minMaxPoint.y > upperLimit
        }

        fun doesAverageChangeTooMuch(sampleRate: Int, samples: List<Int>): Boolean {
            val samplesList: List<Int> =
                if (samples.size < 3 * sampleRate) {
                    return false
                } else samples.subList(
                    samples.size - 3 * sampleRate,
                    samples.size
                )
            var maxAvg = 0
            var minAvg = 10000000
            val amountOfSeconds = 1f
            val window = (sampleRate * amountOfSeconds).roundToInt()
            var i = samplesList.size - 1
            while (i >= window) {
                val sublist = samplesList.subList(i - window, i)
                val avg: Int = getAvg(sublist)
                if (avg < minAvg) minAvg = avg
                if (avg > maxAvg) maxAvg = avg
                i -= window
            }
            return maxAvg.toFloat() / minAvg > 1.2f
        }

        private fun getMinMaxIntoPoint(samples: List<Int>): Point {
            var max = samples[0]
            var min = samples[0]
            for (i in samples) {
                if (i < min) min = i
                if (i > max) max = i
            }
            return Point(min, max)
        }

        private fun getAvg(subList: List<Int>): Int {
            var sum = 0
            for (b in subList) {
                sum += b
            }
            return sum / subList.size
        }

        fun extractHrFromEcgAlgorithm(
            sampleRate: Int,
            samples: List<Int>,
            context: Context?
        ): Int {
            var samples = samples
            val AMOUNT_OF_DATA_IN_SECONDS = 9
            if (samples.size > sampleRate * AMOUNT_OF_DATA_IN_SECONDS) { // sample rate times the amount of seconds of the latest data is checked
                samples = samples.subList(
                    samples.size - sampleRate * AMOUNT_OF_DATA_IN_SECONDS,
                    samples.size
                )
            }
            val movingAvgSamples: List<Int> = getMovingAverage(samples, 5)
            val lPFSamples: List<Int> = subtractLists(movingAvgSamples, samples)
            val squaredLpfOutput: List<Int> = listPow(lPFSamples, 2)

            val movingSummationList: List<Int> =
                getMovingSummation(squaredLpfOutput, sampleRate / 10) //100 ms

            val indexesInAreaOfMovingSummationPeaks: List<Int> = getPeaksAboveThreshold(
                movingSummationList, movingSummationList[getMaxAbsIndex(movingSummationList)] / 4
            )

            val movingSummationPeakIndexes: List<Int> = getLocalMaximasOnTheRight(
                indexesInAreaOfMovingSummationPeaks,
                movingSummationList,
                30
            )

            val theRealPeaks: List<Int> =
                getLocalMaximasInRange(movingSummationPeakIndexes, samples, 30)

            val hrList: List<Int> = getListOfHrValues(theRealPeaks, sampleRate)

            if (hrList.size < 5) return -1
            if (toMuchChangesInHr(hrList)) return -1

            val lastMedian: Double =
                getMedian(hrList.subList(hrList.size - 1 - 4, hrList.size - 1)).toDouble()

            val currentMedian: Double =
                getMedian(hrList.subList(hrList.size - 1 - 3, hrList.size)).toDouble()

            createFiles(samples, movingAvgSamples, movingSummationList, theRealPeaks, context)

            val value = hrList[hrList.size - 1]
            return hrList[hrList.size - 1]
        }

        private fun getMaxAbsIndex(list: List<Int>): Int {
            var max = abs(list[0])
            var maxIndex = 0
            for (i in list.indices) {
                if (abs(list[i]) > max) {
                    max = abs(list[i])
                    maxIndex = i
                }
            }
            return maxIndex
        }

        private fun toMuchChangesInHr(hrList: List<Int>): Boolean {
            var min: Int
            var max: Int
            max = hrList[hrList.size - 4]
            min = max
            var sum = 0
            for (i in hrList.size - 4 until hrList.size) {
                val hr = hrList[i]
                if (hr > max) max = hr
                if (hr < min) min = hr
                sum += hr
            }
            val avgHr = sum / 3
            return max - min > 20
        }

        private fun getListOfHrValues(
            ecgPeakSampleIndexes: List<Int>,
            sampleRate: Int
        ): List<Int> {
            val hrList: MutableList<Int> = ArrayList()
            for (i in 1 until ecgPeakSampleIndexes.size) {
                val calcHr =
                    sampleRate * 60 / (ecgPeakSampleIndexes[i] - ecgPeakSampleIndexes[i - 1]).toDouble()
                hrList.add(calcHr.toInt())
            }
            return hrList
        }

        private fun getMedian(list: List<Int>): Int {
            val cloneList: List<Int> = ArrayList(list)
            Collections.sort(cloneList) { o1: Int?, o2: Int? -> o1!! - o2!! }
            return cloneList[cloneList.size / 2]
        }

        private fun getPeaksAboveThreshold(samples: List<Int>, threshold: Int): List<Int> {
            val ecgPeakIndexes: MutableList<Int> = ArrayList()
            for (i in samples.indices) {
                val delta = samples[i]
                if (delta > threshold) {
                    if (ecgPeakIndexes.size == 0) {
                        ecgPeakIndexes.add(i)
                    } else {
                        if (i - ecgPeakIndexes[ecgPeakIndexes.size - 1] > 39) {
                            ecgPeakIndexes.add(i)
                        }
                    }
                }
            }
            return ecgPeakIndexes
        }

        private fun getMovingAverage(samples: List<Int>, amountOfSamplesToAvg: Int): List<Int> {
            val movingAvgList: MutableList<Int> = ArrayList()
            for (i in 0 until amountOfSamplesToAvg) {
                movingAvgList.add(samples[i])
            }
            for (i in amountOfSamplesToAvg until samples.size - amountOfSamplesToAvg) {
                val offset = amountOfSamplesToAvg / 2
                var sum = 0
                for (j in i - offset until i + (amountOfSamplesToAvg - offset)) {
                    sum += samples[j]
                }
                movingAvgList.add((sum.toFloat() / amountOfSamplesToAvg).roundToInt())
            }
            for (i in movingAvgList.size until samples.size) {
                movingAvgList.add(samples[i])
            }
            return movingAvgList
        }

        private fun multiplyList(samples: List<Int>, mult: Int): List<Int> {
            val multipliedList: MutableList<Int> = ArrayList()
            for (i in samples) {
                multipliedList.add(i * mult)
            }
            return multipliedList
        }

        private fun getLocalMaximasInRange(
            middleIndexes: List<Int>,
            samples: List<Int>,
            range: Int
        ): List<Int> {
            val maximaIndexes: MutableList<Int> = ArrayList()
            val offset = range / 2
            for (i in middleIndexes.indices) {
                if (middleIndexes[i] < offset) // max index >=0 ...
                    continue
                val centerIndex = middleIndexes[i]
                var maxIndex = centerIndex - offset
                var b = centerIndex - offset
                while (b < centerIndex + range - offset - 1 && b < samples.size) {
                    if (samples[b] > samples[maxIndex]) maxIndex = b
                    b++
                }
                if (maximaIndexes.size > 0 && maxIndex - maximaIndexes[maximaIndexes.size - 1] < 30) {
                    if (samples[maxIndex] > samples[maximaIndexes[maximaIndexes.size - 1]]) maximaIndexes[maximaIndexes.size - 1] =
                        maxIndex
                } else {
                    maximaIndexes.add(maxIndex)
                }
            }
            return maximaIndexes
        }

        private fun getLocalMaximasOnTheRight(
            pointsNearPeaks: List<Int>,
            samples: List<Int>,
            range: Int
        ): List<Int> {
            val maximaIndexes: MutableList<Int> = ArrayList()
            for (i in pointsNearPeaks.indices) {
                val startingIndex = pointsNearPeaks[i]
                var maxIndex = startingIndex
                var b = startingIndex
                while (b < startingIndex + range - 1 && b < samples.size) {
                    if (samples[b] > samples[maxIndex]) maxIndex = b
                    b++
                }
                if (maximaIndexes.size > 0 && maxIndex - maximaIndexes[maximaIndexes.size - 1] < 26) {
                    if (samples[maxIndex] > samples[maximaIndexes[maximaIndexes.size - 1]]) maximaIndexes[maximaIndexes.size - 1] =
                        maxIndex
                } else {
                    maximaIndexes.add(maxIndex)
                }
            }
            return maximaIndexes
        }

        private fun getMovingSummation(list: List<Int>, length: Int): List<Int> {
            val offset = length / 2
            val summationList: MutableList<Int> = ArrayList()
            for (i in 0 until offset) {
                summationList.add(0)
            }
            for (i in offset until list.size - offset) {
                var sum = 0
                for (b in i - offset until i + (length - offset)) {
                    sum += list[b]
                }
                summationList.add(sum)
            }
            for (i in list.size - offset until list.size) {
                summationList.add(0)
            }
            return summationList
        }

        private fun listPow(list: List<Int>, pow: Int): List<Int> {
            val poweredList: MutableList<Int> = ArrayList()
            for (i in list) {
                poweredList.add(Math.pow(i.toDouble(), pow.toDouble()).toInt())
            }
            return poweredList
        }

        private fun subtractLists(listA: List<Int>, listB: List<Int>): List<Int> {
            val deltaList: MutableList<Int> = ArrayList()
            for (i in listA.indices) {
                deltaList.add(listA[i] - listB[i])
            }
            return deltaList
        }

        private fun createFiles(
            fileASamples: List<Int>,
            fileBSamples: List<Int>,
            fileCSamples: List<Int>,
            fileDSamples: List<Int>,
            context: Context?
        ) {
            try {
                if (context == null) return
                var ecgFileWriter: ECGFileWriter =
                    EcgFileWriterByLength(fileASamples.size, context, "rawSamples.txt")
                ecgFileWriter.writeToFile(fileASamples)
                ecgFileWriter =
                    EcgFileWriterByLength(fileBSamples.size, context, "movingAvgSamples.txt")
                ecgFileWriter.writeToFile(fileBSamples)
                ecgFileWriter =
                    EcgFileWriterByLength(fileCSamples.size, context, "movingSummation.txt")
                ecgFileWriter.writeToFile(fileCSamples)
                ecgFileWriter = EcgFileWriterByLength(fileDSamples.size, context, "peakIndexes.txt")
                ecgFileWriter.writeToFile(fileDSamples)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}