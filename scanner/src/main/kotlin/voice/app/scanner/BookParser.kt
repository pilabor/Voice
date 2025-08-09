package voice.app.scanner

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toFile
import dev.zacsweers.metro.Inject
import voice.common.BookId
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.legacy.LegacyBookSettings
import voice.data.repo.BookContentRepo
import voice.data.repo.internals.dao.LegacyBookDao
import voice.data.toUri
import voice.documentfile.CachedDocumentFile
import voice.documentfile.CachedDocumentFileFactory
import voice.logging.core.Logger
import java.io.File
import java.time.Instant

import java.io.RandomAccessFile

@Inject
class BookParser(
  private val contentRepo: BookContentRepo,
  private val mediaAnalyzer: MediaAnalyzer,
  private val legacyBookDao: LegacyBookDao,
  private val application: Application,
  private val bookmarkMigrator: BookmarkMigrator,
  private val fileFactory: CachedDocumentFileFactory,
) {

  suspend fun parseAndStore(
    chapters: List<Chapter>,
    file: CachedDocumentFile,
  ): BookContent {
    val id = BookId(file.uri)
    return contentRepo.getOrPut(id) {
      val uri = chapters.first().id.toUri()
      val analyzed = mediaAnalyzer.analyze(fileFactory.create(uri))




      val filePath = file.uri.filePath()
      val migrationMetaData = filePath?.let {
        legacyBookDao.bookMetaData()
          .find { metadata -> metadata.root.endsWith(it) }
      }

      val movementName = analyzed?.series

      var name = migrationMetaData?.name
        ?: analyzed?.album
        ?: analyzed?.title
        ?: file.bookName()

      if(!movementName.isNullOrBlank()) {
        name += "$movementName: $name"
      }

      val migrationSettings = migrationMetaData?.let {
        legacyBookDao.settingsById(it.id)
      }

      if (migrationMetaData != null) {
        bookmarkMigrator.migrate(migrationMetaData, chapters, id)
      }

      val migratedPlaybackPosition = migrationSettings?.let { findMigratedPlaybackPosition(it, chapters) }
      BookContent(
        id = id,
        isActive = true,
        addedAt = migrationMetaData?.addedAtMillis?.let(Instant::ofEpochMilli)
          ?: Instant.now(),
        author = analyzed?.artist,
        lastPlayedAt = migrationSettings?.lastPlayedAtMillis?.let(Instant::ofEpochMilli)
          ?: Instant.EPOCH,
        name = name,
        playbackSpeed = migrationSettings?.playbackSpeed
          ?: 1F,
        skipSilence = migrationSettings?.skipSilence
          ?: false,
        chapters = chapters.map { it.id },
        positionInChapter = migratedPlaybackPosition?.playbackPosition ?: 0L,
        currentChapter = migratedPlaybackPosition?.chapterId ?: chapters.first().id,
        cover = migrationSettings?.id?.let {
          File(application.filesDir, id.toString())
            .takeIf { it.canRead() }
        },
        gain = 0F,
      ).also {
        validateIntegrity(it, chapters)
      }
    }
  }

  private fun findMigratedPlaybackPosition(
    settings: LegacyBookSettings,
    chapters: List<Chapter>,
  ): MigratedPlaybackPosition? {
    val currentChapter = chapters.find {
      val chapterFilePath = it.id.toUri().filePath()
      if (chapterFilePath == null) {
        false
      } else {
        settings.currentFile.absolutePath.endsWith(chapterFilePath)
      }
    } ?: return null
    return MigratedPlaybackPosition(currentChapter.id, settings.positionInChapter)
  }

  private data class MigratedPlaybackPosition(
    val chapterId: ChapterId,
    val playbackPosition: Long,
  )

  private fun CachedDocumentFile.bookName(): String {
    val fileName = name
    return if (fileName == null) {
      uri.toString()
        .removePrefix("/storage/emulated/0/")
        .removePrefix("/storage/emulated/")
        .removePrefix("/storage/")
        .also {
          Logger.w("Could not parse fileName from $this. Fallback to $it")
        }
    } else {
      if (isFile) {
        fileName.substringBeforeLast(".")
      } else {
        fileName
      }
    }
  }
}

internal fun validateIntegrity(
  content: BookContent,
  chapters: List<Chapter>,
) {
  // the init block performs integrity validation
  Book(content, chapters)
}

internal fun Uri.filePath(): String? {
  return pathSegments.lastOrNull()
    ?.dropWhile { it != ':' }
    ?.removePrefix(":")
}
/*

private fun BookParser.parseMovementName(cachedFile:CachedDocumentFile):String {
  val file = cachedFile.uri.toFile()

  val raf = RandomAccessFile(file, "r")
  return parseAtoms(raf)
}

fun parseAtoms(raf: RandomAccessFile):String {
  var atomStartPos: Long
  var atomSize: Int
  var atomType: String

  while (raf.filePointer < raf.length()) {
    // Read the size of the atom (4 bytes)
    atomSize = raf.readInt()

    // Read the type of the atom (4 bytes)
    val typeBytes = ByteArray(4)
    raf.readFully(typeBytes)
    atomType = String(typeBytes)

    // Print the atom's size and type
    // println("Found atom: $atomType, size: $atomSize")

    // If the atom type is "moov", check its subatoms
    if (atomType == "moov") {
      val moovAtomStart = raf.filePointer
      return parseMoovAtom(raf, moovAtomStart, atomSize)
    } else {
      // Skip the content of the atom
      raf.seek(raf.filePointer + atomSize - 8)  // size - 8 to skip size and type bytes
    }
  }
  return ""
}

fun parseMoovAtom(raf: RandomAccessFile, moovStart: Long, moovSize: Int):String {
  val moovEnd = moovStart + moovSize
  while (raf.filePointer < moovEnd) {
    // Read atom size
    val atomSize = raf.readInt()

    // Read atom type (4 bytes)
    val typeBytes = ByteArray(4)
    raf.readFully(typeBytes)
    val atomType = String(typeBytes)

    // If we find the "©mvn" atom, process it
    if (atomType == "©mvn") {
      val mvnData = ByteArray(atomSize - 8)  // subtract size/type bytes
      raf.readFully(mvnData)
      // println("Found ©mvn atom with data: ${String(mvnData)}")
      return String(mvnData)
    } else {
      // Skip the content of other atoms
      raf.seek(raf.filePointer + atomSize - 8)
    }

  }

  return ""
}
*/
