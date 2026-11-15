package com.artemidius.bloodpressure.ml

import dev.sevendigits.BoundingBox
import dev.sevendigits.RecognitionResult
import dev.sevendigits.RecognitionSource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Assigns measurement roles after OCR. Scores are evidence weights, not probabilities. */
object PressureReadingExtractor {
    enum class Outcome { MATCHED, NO_CANDIDATES, NO_PLAUSIBLE_PAIR }
    data class Extraction(
        val reading: ImageData? = null,
        val outcome: Outcome,
        val candidateCount: Int,
        val bestScore: Float = 0f,
        val margin: Float = 0f
    )

    private enum class Role { SYS, DIA, PULSE, OTHER, UNIT }
    private data class Field(val text: String, val box: BoundingBox, val row: Int, val digitConfidence: Float?)
    private data class Number(val field: Field, val value: Int, val roles: MutableSet<Role> = mutableSetOf())
    private data class PairScore(val sys: Number, val dia: Number, val score: Float)
    private val integer = Regex("[0-9]{1,3}")
    // Broad scan acceptance bounds, not normal ranges or a definition of viability.
    private val systolicRange = 30..300
    private val diastolicRange = 10..250

    fun extract(result: RecognitionResult, rotationDegrees: Int = 0): Extraction {
        require(rotationDegrees in setOf(0, 90, 180, 270))
        val fields = result.lines.flatMapIndexed { row, regions -> regions.map { region ->
            Field(region.text.uppercase(), upright(region.boundingBox, result, rotationDegrees), row,
                region.characters.filter { it.source == RecognitionSource.SEVEN_SEGMENT }
                    .minOfOrNull { it.confidence })
        } }
        val numbers = fields.filter { integer.matches(it.text) }.mapNotNull { field ->
            field.text.toInt().takeIf { it in 1..300 }?.let { Number(field, it) }
        }.distinctBy { it.field.text to it.field.box }
        if (numbers.size < 2) return Extraction(outcome = Outcome.NO_CANDIDATES, candidateCount = numbers.size)
        // Bound pair searches on cluttered photos, preferring larger numeric fields.
        val candidates = numbers.sortedByDescending { it.field.box.height }.take(80)
            .sortedWith(compareBy({ it.field.row }, { it.field.box.left }))

        assignLabels(fields, numbers)
        excludeSplitClocksAndDates(fields, numbers)
        val pairs = candidates.flatMap { sys -> candidates.mapNotNull { dia ->
            if (sys === dia) null else score(sys, dia, candidates)
        } }.sortedByDescending { it.score }
        val best = pairs.firstOrNull()
            ?: return Extraction(outcome = Outcome.NO_PLAUSIBLE_PAIR, candidateCount = numbers.size)
        val margin = best.score - (pairs.getOrNull(1)?.score ?: 0f)
        return Extraction(
            reading = ImageData(best.sys.value, best.dia.value),
            outcome = Outcome.MATCHED, candidateCount = numbers.size, bestScore = best.score, margin = margin
        )
    }

    private fun role(text: String): Role? = when (text.trim('.', ':')) {
        "SYS", "SYSTOLIC" -> Role.SYS
        "DIA", "DIASTOLIC" -> Role.DIA
        "PUL", "PULSE", "BPM", "HR", "PR", "HEARTRATE" -> Role.PULSE
        "TIME", "DATE", "MEM", "MEMORY", "YEAR", "$", "€", "£" -> Role.OTHER
        "MMHG" -> Role.UNIT
        else -> null
    }

    private fun assignLabels(fields: List<Field>, numbers: List<Number>) {
        for (label in fields) {
            val role = role(label.text) ?: continue
            val neighbors = numbers.mapNotNull { number -> labelDistance(label.box, number.field.box)?.let { number to it } }
                .sortedBy { it.second }
            val closest = neighbors.firstOrNull() ?: continue
            // An equidistant label cannot choose between two measurements.
            if (neighbors.size > 1 && neighbors[1].second - closest.second < .3f) {
                if (role != Role.UNIT) neighbors.filter { it.second - closest.second < .3f }
                    .forEach { it.first.roles += Role.OTHER }
                continue
            }
            closest.first.roles += role
        }
    }

    private fun labelDistance(label: BoundingBox, number: BoundingBox): Float? {
        val h = number.height
        val dy = abs(label.cy - number.cy) / h
        val xGap = max(0f, max(label.left - number.right, number.left - label.right)) / h
        if (dy <= .8f && xGap <= 3f) return xGap + dy * 2f
        // Labels above/below a numeric field are common in horizontal layouts.
        val yGap = max(0f, max(label.top - number.bottom, number.top - label.bottom)) / h
        if (overlapX(label, number) >= .35f && yGap <= .7f) return 1f + yGap + abs(label.cx - number.cx) / h
        return null
    }

    private fun excludeSplitClocksAndDates(fields: List<Field>, numbers: List<Number>) {
        for (separator in fields.filter { it.text in setOf(":", "/", "-", ".") }) {
            // Exclude both sides of e.g. ["12", ":", "30"], including across OCR row splits.
            val before = numbers.filter { it.field.box.right <= separator.box.left &&
                abs(it.field.box.cy - separator.box.cy) < it.field.box.height * .6f &&
                separator.box.left - it.field.box.right < it.field.box.height }.maxByOrNull { it.field.box.right }
            val after = numbers.filter { it.field.box.left >= separator.box.right &&
                abs(it.field.box.cy - separator.box.cy) < it.field.box.height * .6f &&
                it.field.box.left - separator.box.right < it.field.box.height }.minByOrNull { it.field.box.left }
            if (before != null && after != null) { before.roles += Role.OTHER; after.roles += Role.OTHER }
        }
    }

    private fun score(sys: Number, dia: Number, all: List<Number>): PairScore? {
        if (sys.value !in systolicRange || dia.value !in diastolicRange || sys.value <= dia.value) return null
        var penalty = 0f
        if (sys.roles.any { it == Role.DIA || it == Role.PULSE || it == Role.OTHER }) penalty += 6f
        if (dia.roles.any { it == Role.SYS || it == Role.PULSE || it == Role.OTHER }) penalty += 6f
        // The detector already applies its own character threshold. Its scores are not
        // calibrated probabilities: a second hard cutoff rejects correct digits (e.g. 118
        // at 0.342 in the supplied monitor photo). Use a small continuous penalty instead.
        val uncertaintyPenalty = 1f - min(sys.field.digitConfidence ?: 1f, dia.field.digitConfidence ?: 1f)
        val sysLabel = Role.SYS in sys.roles
        val diaLabel = Role.DIA in dia.roles
        val units = Role.UNIT in sys.roles || Role.UNIT in dia.roles
        if (sysLabel && !diaLabel && all.any { Role.DIA in it.roles && nearby(sys.field.box, it.field.box) }) penalty += 3f
        if (diaLabel && !sysLabel && all.any { Role.SYS in it.roles && nearby(dia.field.box, it.field.box) }) penalty += 3f
        val s = sys.field.box
        val d = dia.field.box
        val height = (s.height + d.height) / 2f
        val similarity = min(s.height, d.height) / max(s.height, d.height)

        val vertical = abs(d.cy - s.cy) >= height * .65f
        if (vertical && !columnAligned(s, d)) penalty += 2f
        if (!(sysLabel && diaLabel) && (if (vertical) s.cy >= d.cy else s.cx >= d.cx)) penalty += 4f
        val gap = if (vertical) max(s.top, d.top) - min(s.bottom, d.bottom)
            else max(s.left, d.left) - min(s.right, d.right)
        val relativeGap = gap / height
        if (relativeGap < -.2f) penalty += 2f
        penalty += (relativeGap - 2f).coerceIn(0f, 4f)
        // Skipped comparable fields lower the score, but never veto the only plausible pair.
        if (all.any { other -> other !== sys && other !== dia && other.field.box.height >= min(s.height, d.height) * .65f &&
            if (vertical) columnAligned(s, other.field.box) && other.field.box.cy > min(s.cy, d.cy) && other.field.box.cy < max(s.cy, d.cy)
            else abs(other.field.box.cy - s.cy) < height * .6f && other.field.box.cx > min(s.cx, d.cx) && other.field.box.cx < max(s.cx, d.cx)
        }) penalty += 2f

        val stack = if (vertical && s.cy < d.cy) hasPulseBelow(sys, dia, all) else false
        // Labels, complete stacks and separation from alternatives improve ranking only.
        // The user prefers an editable best guess over a rejection when a plausible pair exists.
        val alignment = if (vertical) max(overlapX(s, d), 1f - abs(s.right - d.right) / height).coerceIn(0f, 1f)
            else (1f - abs(s.cy - d.cy) / height).coerceIn(0f, 1f)
        val largest = all.maxOf { it.field.box.height }
        val prominence = (min(s.height, d.height) / largest).coerceAtMost(1f)
        val rowAgreement = if (vertical) sys.field.row < dia.field.row else sys.field.row == dia.field.row
        val score = 2f * similarity + 2f * alignment + (1.5f - relativeGap.coerceAtLeast(0f) * .4f).coerceAtLeast(0f) +
            1.5f * prominence + (if (rowAgreement) .5f else 0f) +
            (if (sysLabel) 3f else 0f) + (if (diaLabel) 3f else 0f) +
            (if (units) 1.5f else 0f) + (if (stack) 2f else 0f) - uncertaintyPenalty - penalty
        return PairScore(sys, dia, score)
    }

    private fun hasPulseBelow(sys: Number, dia: Number, all: List<Number>): Boolean {
        val s = sys.field.box
        val d = dia.field.box
        val h = (s.height + d.height) / 2f
        val below = all.filter { it !== sys && it !== dia && it.value in 1..300 &&
            it.roles.none { role -> role == Role.SYS || role == Role.DIA || role == Role.OTHER } &&
            it.field.box.cy > d.cy && it.field.box.top - d.bottom in (-.1f * h)..(2f * h) &&
            it.field.box.height in (.35f * h)..(1.15f * h) && columnAligned(d, it.field.box)
        }
        if (below.size != 1) return false
        val pulse = below.single().field.box
        // Reject four equally prominent stacked fields: one might be a clock/memory value.
        return all.none { other -> other !== sys && other !== dia && other !== below.single() &&
            Role.OTHER !in other.roles && columnAligned(s, other.field.box) &&
            other.field.box.cy in (s.top - 2f * h)..(pulse.bottom + 2f * h) &&
            (other.field.box.height >= min(s.height, d.height) * .65f ||
                (other.field.box.cy > pulse.cy && other.field.box.height >= .35f * h))
        }
    }

    private fun nearby(a: BoundingBox, b: BoundingBox): Boolean {
        val h = max(a.height, b.height)
        return abs(a.cy - b.cy) < 5f * h && abs(a.cx - b.cx) < 5f * h
    }
    private fun columnAligned(a: BoundingBox, b: BoundingBox): Boolean =
        overlapX(a, b) >= .45f || abs(a.right - b.right) <= min(a.height, b.height) * .65f
    private fun overlapX(a: BoundingBox, b: BoundingBox): Float =
        (min(a.right, b.right) - max(a.left, b.left)).coerceAtLeast(0f) / min(a.width, b.width)
    private val BoundingBox.cx get() = (left + right) / 2f
    private val BoundingBox.cy get() = (top + bottom) / 2f

    private fun upright(b: BoundingBox, result: RecognitionResult, rotation: Int): BoundingBox = when (rotation) {
        90 -> BoundingBox(result.imageHeight - b.bottom, b.left, result.imageHeight - b.top, b.right)
        180 -> BoundingBox(result.imageWidth - b.right, result.imageHeight - b.bottom, result.imageWidth - b.left, result.imageHeight - b.top)
        270 -> BoundingBox(b.top, result.imageWidth - b.right, b.bottom, result.imageWidth - b.left)
        else -> b
    }
}
