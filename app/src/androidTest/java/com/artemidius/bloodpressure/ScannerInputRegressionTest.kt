package com.artemidius.bloodpressure

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artemidius.bloodpressure.ml.ImageData
import com.artemidius.bloodpressure.ml.ScanImageRepositoryImpl
import com.artemidius.bloodpressure.ml.TextRecognizerImpl
import com.artemidius.bloodpressure.ml.decodePhoto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in local regression: the supplied photo stays in app cache, outside source/APKs. */
@RunWith(AndroidJUnit4::class)
class ScannerInputRegressionTest {
    @Test fun suppliedPhotoProducesExpectedPressure() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Pass scannerRegression=true and stage scanner-regression.jpg in app cache", args.getString("scannerRegression") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.fromFile(File(context.cacheDir, "scanner-regression.jpg"))
        val expected = ImageData(args.getString("expectedSys")!!.toInt(), args.getString("expectedDia")!!.toInt())
        val bitmap = decodePhoto(context.contentResolver, uri)
        try {
            assertEquals("Decoded ${bitmap.width}x${bitmap.height}", expected,
                TextRecognizerImpl(context).recognize(bitmap, 0))
        } finally { bitmap.recycle() }
        assertEquals(expected, ScanImageRepositoryImpl(context, TextRecognizerImpl(context)).recognize(uri))
    }
}
