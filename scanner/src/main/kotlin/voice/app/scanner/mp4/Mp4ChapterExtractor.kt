package voice.app.scanner.mp4

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorInput
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.data.MarkData
import voice.logging.core.Logger
import java.io.IOException

data class Mp4Metadata(val movementName:String?, val chapters: List<MarkData>)
@Inject
class Mp4ChapterExtractor(
  private val context: Context,
  private val boxParser: Mp4BoxParser,
  private val chapterTrackProcessor: ChapterTrackProcessor,
) {

  suspend fun extractMp4Metadata(uri: Uri): Mp4Metadata = withContext(Dispatchers.IO) {
    val dataSource = DefaultDataSource.Factory(context).createDataSource()
    dataSource.open(DataSpec(uri))
    var m = Mp4Metadata("", emptyList())
    try {
      val input = DefaultExtractorInput(dataSource, 0, C.LENGTH_UNSET.toLong())
      val topLevelResult = boxParser(input)

      val trackId = topLevelResult.chapterTrackId
      val chapters = when {
        topLevelResult.chplChapters.isNotEmpty() -> {
          topLevelResult.chplChapters
        }
        trackId != null -> {
          chapterTrackProcessor(uri, dataSource, trackId, topLevelResult)
        }
        else -> emptyList()
      }

      m = Mp4Metadata(topLevelResult.movementName, chapters)
    } catch (e: IOException) {
      Logger.w(e, "Failed to open MP4 file for chapter extraction")
    } catch (e: SecurityException) {
      Logger.w(e, "Security exception while accessing MP4 file")
    } catch (e: IllegalStateException) {
      Logger.w(e, "Invalid MP4 structure")
    } catch (e: ArrayIndexOutOfBoundsException) {
      Logger.w(e, "Undeclared")
      // https://github.com/androidx/media/issues/2467
    } finally {
      try {
        dataSource.close()
      } catch (e: IOException) {
        Logger.w(e, "Error closing data source")
      }
    }
    m
  }


  suspend fun extractChapters(uri: Uri): List<MarkData> = withContext(Dispatchers.IO) {
      extractMp4Metadata(uri).chapters
  }
}
