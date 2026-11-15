package com.artemidius.bloodpressure

import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.ml.PressureReadingExtractor
import dev.sevendigits.*
import org.junit.Assert.*
import org.junit.Test

class PressureReadingExtractorTest {
    private fun region(text: String, x: Float, y: Float, w: Float = 150f, h: Float = 100f,
                       confidence: Float = .9f): TextRegion = TextRegion(text.mapIndexed { i, c ->
        RecognizedCharacter(c, BoundingBox(x + i * w / text.length, y, x + (i + 1) * w / text.length, y + h), confidence)
    })
    private fun result(vararg rows: List<TextRegion>) = RecognitionResult(2000, 2200, rows.toList())
    private fun reading(vararg fields: TextRegion): ImageData? =
        PressureReadingExtractor.extract(result(*fields.map { listOf(it) }.toTypedArray())).reading
    private fun sys(x: Float = 500f, y: Float = 500f) = region("118", x, y)
    private fun dia(x: Float = 550f, y: Float = 640f) = region("78", x, y, 100f)
    private fun pulse(x: Float = 580f, y: Float = 780f) = region("70", x, y, 70f, 65f)
    private fun label(text: String, x: Float, y: Float) = region(text, x, y, 65f, 25f)

    @Test fun `user reported 118 78 70 layout assigns the upper two fields`() {
        // Text from the user's scanner output; SYS box from its visual output,
        // remaining boxes approximate the displayed monitor layout (not an original-photo fixture).
        assertEquals(ImageData(118, 78), reading(
            region("118", 612f, 1053f, 235f, 136f),
            region("78", 650f, 1210f, 197f, 136f),
            region("70", 725f, 1370f, 122f, 80f)
        ))
    }
    @Test fun `clock date and model text do not displace the pressure cluster`() {
        assertEquals(ImageData(118, 78), reading(
            region("07:30", 520f, 380f, 120f, 30f), region("11/15", 800f, 380f, 100f, 30f),
            sys(), region("HEM-6130", 80f, 40f, 180f, 25f), dia(), pulse(),
            region("25", 1500f, 120f, 40f, 25f)
        ))
    }
    @Test fun `a small clock with missing colon is not a large pressure field`() {
        assertEquals(ImageData(118, 78), reading(region("123", 580f, 420f, 65f, 25f), sys(), dia(), pulse()))
    }
    @Test fun `pulse can exceed both pressure values`() {
        assertEquals(ImageData(118, 78), reading(sys(), dia(), region("160", 550f, 780f, 100f, 65f)))
    }
    @Test fun `pulse being closer to diastolic does not select the bottom pair`() {
        assertEquals(ImageData(118, 78), reading(sys(), dia(), pulse(y = 750f)))
    }
    @Test fun `three same sized aligned fields use layout rather than sorting values`() {
        assertEquals(ImageData(118, 78), reading(sys(), dia(), region("70", 550f, 780f, 100f)))
    }
    @Test fun `four similarly prominent fields still provide the best guess`() {
        assertNotNull(reading(region("130", 500f, 360f), sys(), dia(), region("70", 550f, 780f, 100f)))
    }
    @Test fun `an uncertain unpunctuated clock does not block all suggestions`() {
        assertNotNull(reading(region("130", 500f, 360f), sys(), dia(), pulse()))
    }
    @Test fun `equidistant pressure labels do not block a best guess`() {
        assertNotNull(reading(label("SYS", 400f, 605f), sys(), region("78", 500f, 640f),
            region("70", 500f, 780f)))
    }
    @Test fun `two unlabelled numbers now populate the draft`() {
        assertEquals(ImageData(78, 70), reading(dia(), pulse()))
        assertEquals(ImageData(118, 78), reading(sys(), dia()))
    }
    @Test fun `labels allow pressure without a detected pulse`() {
        assertEquals(ImageData(118, 78), reading(label("SYS.", 400f, 530f), sys(), label("DIA", 450f, 670f), dia()))
    }
    @Test fun `a lone systolic label can support a two number suggestion`() {
        assertEquals(ImageData(118, 78), reading(label("SYS", 400f, 530f), sys(), dia()))
    }
    @Test fun `a complete stack tolerates one missing pressure label`() {
        assertEquals(ImageData(118, 78), reading(label("SYS", 400f, 530f), sys(), dia(), pulse()))
    }
    @Test fun `a pressure label and units identify the two separate fields`() {
        assertEquals(ImageData(118, 78), reading(label("SYS", 400f, 530f), sys(), dia(), label("mmHg", 420f, 670f)))
    }
    @Test fun `even conflicting labels allow the only plausible pair`() {
        assertEquals(ImageData(78, 70), reading(label("DIA", 450f, 670f), dia(), label("PULSE", 450f, 790f), pulse()))
    }
    @Test fun `units support a pair when pressure labels were not recognized`() {
        assertEquals(ImageData(118, 78), reading(sys(), dia(), label("mmHg", 380f, 530f), label("mmHg", 420f, 670f)))
    }
    @Test fun `one units label is sufficient to suggest a plausible pair`() {
        assertEquals(ImageData(78, 70), reading(region("78", 500f, 500f), region("70", 500f, 640f), label("mmHg", 380f, 530f)))
    }
    @Test fun `nearby explicit labels take precedence over intervening noise`() {
        assertEquals(ImageData(118, 78), reading(label("SYS", 400f, 530f), sys(), region("90", 550f, 620f, 100f),
            label("DIA", 450f, 850f), dia(y = 820f)))
    }
    @Test fun `unlabelled horizontal integers cannot distinguish pressure from a clock`() {
        assertNull(reading(region("23", 100f, 100f), region("12", 300f, 100f)))
    }
    @Test fun `horizontal labels above the numbers are supported`() {
        val r = result(listOf(label("SYS", 520f, 450f), label("DIA", 820f, 450f)),
            listOf(region("118", 500f, 500f), region("78", 800f, 500f)))
        assertEquals(ImageData(118, 78), PressureReadingExtractor.extract(r).reading)
    }
    @Test fun `explicit labels can override left to right assumptions`() {
        val r = result(listOf(label("DIA", 520f, 450f), label("SYS", 820f, 450f)),
            listOf(region("78", 500f, 500f), region("118", 800f, 500f)))
        assertEquals(ImageData(118, 78), PressureReadingExtractor.extract(r).reading)
    }
    @Test fun `split time punctuation rejects the associated integers`() {
        assertNull(reading(label("SYS", 20f, 120f), region("23", 100f, 100f),
            region(":", 260f, 130f, 15f, 40f), region("12", 300f, 100f), label("DIA", 480f, 120f)))
    }
    @Test fun `tied monitor groups return a stable best guess`() {
        val r = result(listOf(sys(), sys(x = 1200f)), listOf(dia(), dia(x = 1250f)),
            listOf(pulse(), pulse(x = 1280f)))
        val extraction = PressureReadingExtractor.extract(r)
        assertEquals(ImageData(118, 78), extraction.reading)
        assertEquals(PressureReadingExtractor.Outcome.MATCHED, extraction.outcome)
        assertEquals(0f, extraction.margin, .001f)
    }
    @Test fun `explicit labels can outweigh an intervening field`() {
        assertEquals(ImageData(118, 78), reading(label("SYS", 400f, 530f), sys(), region("90", 550f, 640f, 100f),
            label("DIA", 450f, 810f), dia(y = 780f)))
    }
    @Test fun `confidence is a soft signal and does not veto strong field evidence`() {
        val low = PressureReadingExtractor.extract(result(listOf(label("SYS", 400f, 530f),
            region("118", 500f, 500f, confidence = .3f)), listOf(label("DIA", 450f, 670f), dia())))
        val high = PressureReadingExtractor.extract(result(listOf(label("SYS", 400f, 530f), sys()),
            listOf(label("DIA", 450f, 670f), dia())))
        assertEquals(ImageData(118, 78), low.reading)
        assertEquals(low.reading, high.reading)
        assertTrue(low.bestScore < high.bestScore)
    }
    @Test fun `actual supplied photo OCR with 0342 confidence still extracts 118 over 78`() {
        val visual = result(
            listOf(region("sense", 230f, 80f, 131f, 32f), region("Intelli", 237f, 20f, 128f, 74f)),
            listOf(region("7:30", 412f, 252f, 236f, 92f)),
            listOf(region("S", 17f, 333f, 46f, 64f), region("118", 195.07922f, 360.2412f, 479.31898f, 267.80714f, .34231064f)),
            listOf(region("A", 0f, 625f, 113f, 76f), region("78", 301.83316f, 647.76636f, 376.45798f, 271.22448f, .77573365f)),
            listOf(region("SE", 12f, 928f, 58f, 44f), region("70", 436.974f, 941.2755f, 231.73236f, 169.5291f, .86851805f))
        )
        assertEquals(ImageData(118, 78), PressureReadingExtractor.extract(visual).reading)
    }
    @Test fun `invalid signed decimal or leading zero readings are not repaired`() {
        for (text in listOf("-118", "118.5", "0118", "O18", "0", "301")) {
            assertNull(text, reading(label("SYS", 400f, 530f), region(text, 500f, 500f), label("DIA", 450f, 670f), dia()))
        }
    }
    @Test fun `an unusual ordering still yields a plausible suggestion`() {
        assertNotNull(reading(region("78", 500f, 500f), region("118", 500f, 640f), pulse()))
    }
    @Test fun `geometry matters more than consecutive positions in the list`() {
        assertEquals(ImageData(118, 78), reading(sys(), region("130", 1500f, 520f), dia(),
            region("85", 1500f, 660f), pulse()))
    }
    @Test fun `scale and translation do not change the selection`() {
        val fields = listOf(sys(), dia(), pulse())
        for (scale in listOf(.3f, .75f, 1.4f)) {
            val transformed = fields.map { f -> TextRegion(f.characters.map { c ->
                val b = c.boundingBox
                c.copy(boundingBox = BoundingBox(b.left * scale + 50, b.top * scale + 100,
                    b.right * scale + 50, b.bottom * scale + 100))
            }) }
            assertEquals(ImageData(118, 78), reading(*transformed.toTypedArray()))
        }
    }
    @Test fun `logged two remaining candidates are shown even if systolic OCR is unusable`() {
        // The log masks the digit values. Model the missing/invalid upper field as 00,
        // and the two readable fields as the known monitor DIA/PULSE values.
        val visual = result(
            listOf(region("7:30", 653f, 391f, 400f, 165f)),
            listOf(region("00", 615f, 574f, 463f, 428f)),
            listOf(region("78", 488f, 1034f, 595f, 433f)),
            listOf(region("70", 706f, 1500f, 361f, 271f))
        )
        assertEquals(ImageData(78, 70), PressureReadingExtractor.extract(visual).reading)
    }
    @Test fun `a pair can be accepted below the old score threshold`() {
        val extraction = PressureReadingExtractor.extract(result(listOf(region("118", 50f, 50f)),
            listOf(region("78", 1500f, 1700f, 50f, 30f))))
        assertEquals(ImageData(118, 78), extraction.reading)
        assertTrue(extraction.bestScore < 8f)
    }
    @Test fun `scan bounds exclude impossible looking values without clamping`() {
        for ((sys, dia) in listOf(29 to 20, 301 to 80, 120 to 9, 280 to 251, 80 to 80)) {
            assertNull("$sys/$dia", reading(region(sys.toString(), 500f, 500f), region(dia.toString(), 500f, 640f)))
        }
        assertEquals(ImageData(30, 10), reading(region("30", 500f, 500f), region("10", 500f, 640f)))
        assertEquals(ImageData(300, 250), reading(region("300", 500f, 500f), region("250", 500f, 640f)))
    }
    @Test fun `leading zero integer fields are usable without inventing missing digits`() {
        assertEquals(ImageData(118, 78), reading(sys(), region("078", 500f, 640f)))
    }
    @Test fun `original image boxes are brought upright for all camera rotations`() {
        val fields = listOf(sys(), dia(), pulse())
        for (rotation in listOf(90, 180, 270)) {
            val transformed = fields.map { f -> TextRegion(f.characters.map { c ->
                val b = c.boundingBox
                val rotated = when (rotation) {
                    90 -> BoundingBox(b.top, 2000 - b.right, b.bottom, 2000 - b.left)
                    180 -> BoundingBox(2000 - b.right, 2200 - b.bottom, 2000 - b.left, 2200 - b.top)
                    else -> BoundingBox(2200 - b.bottom, b.left, 2200 - b.top, b.right)
                }
                c.copy(boundingBox = rotated)
            }) }
            val r = RecognitionResult(if (rotation == 180) 2000 else 2200,
                if (rotation == 180) 2200 else 2000, transformed.map { listOf(it) })
            assertEquals(ImageData(118, 78), PressureReadingExtractor.extract(r, rotation).reading)
        }
    }
}
