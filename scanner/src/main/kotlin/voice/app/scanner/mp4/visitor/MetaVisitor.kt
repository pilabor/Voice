package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput
import java.nio.ByteBuffer

// import java.nio.ByteBuffer

fun Int.reverseBytes(): Int {
  val v0 = ((this ushr 0) and 0xFF)
  val v1 = ((this ushr 8) and 0xFF)
  val v2 = ((this ushr 16) and 0xFF)
  val v3 = ((this ushr 24) and 0xFF)
  return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
}

data class MetaAtom(val name:String,val position:Int, val size: Int) {
  val end: Int
    get() = position + size
  val children = mutableListOf<MetaAtom>()
}

// https://atomicparsley.sourceforge.net/mpeg-4files.html
@Inject
class MetaVisitor : AtomVisitor {
  override val path: List<String> = listOf("moov", "udta", "meta")

  val nameField = "©nam"
  val titleField = "titl"
  val albumField = "©alb"
  val artistField = "©ART"
  val artistField2 = "©art"
  val commentField = "©cmt"
  val recordingDateOrYearField = "©day"
  val genreField = "©gen"
  val genreField2 = "gnre"
  val trackNumberTotalField = "trkn"
  val diskNumberTotalField = "disk"
  val ratingField = "rtng"
  val ratingField2 = "rate"
  val composerField = "©wrt"
  val descriptionField = "desc"
  val longDescriptionField = "©des"
  val longDescriptionField2 = "ldes"
  val copyrightField = "cprt"
  val albumArtField = "aART"
  val lyricsField = "©lyr"
  val publisherField = "©pub"
  val publishingDateField = "rldt"
  val productIdField = "prID"
  val conductorField = "©con"
  val sortAlbumField = "soal"
  val sortAlbumArtistField = "soaa"
  val sortArtistField = "soar"
  val sortTitleField = "sonm"
  val groupField = "©grp"
  val movementIndexField = "©mvi"
  val movementNameField = "©mvn"
  val bpmField = "tmpo"
  val encodedByField = "©enc"
  val encodingToolField = "©too"
  val isrcField = "©isr"
  val customField = "----"
  /*
  todo
  { "LANGUAGE", Field.LANGUAGE }, // aka ----:com.apple.iTunes:LANGUAGE
  { "©isr", Field.ISRC },
  { "CATALOGNUMBER", Field.CATALOG_NUMBER }, // aka ----:com.apple.iTunes:CATALOGNUMBER
  { "LYRICIST", Field.LYRICIST } // aka ----:com.apple.iTunes:LYRICIST

   */

  var name: String? = null
  var title: String? = null
  var album: String? = null
  var artist: String? = null
  var comment: String? = null
  var recordingDateOrYear: String? = null
  var genre: String? = null
  var trackNumberTotal: String? = null
  var diskNumberTotal: String? = null
  var rating: String? = null
  var composer: String? = null
  var description: String? = null
  var longDescription: String? = null
  var copyright: String? = null
  var albumArt: String? = null
  var lyrics: String? = null
  var publisher: String? = null
  var publishingDate: String? = null
  var productId: String? = null
  var conductor: String? = null
  var sortAlbum: String? = null
  var sortAlbumArtist: String? = null
  var sortArtist: String? = null
  var sortTitle: String? = null
  var group: String? = null
  var movementIndex: String? = null
  var movementName: String? = null
  var bpm: String? = null
  var encodedBy: String? = null
  var encodingTool: String? = null
  var isrc: String? = null



  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {
    val positionBeforeParsing = buffer.position

    val metaVersionAndFlags = buffer.readString(4)


    val parentAtom = MetaAtom("meta", buffer.position, buffer.data.size)
    // try {
      parseAtoms(buffer, parentAtom)
    // } catch(e: Exception) {
    //  var x = "x"
    //}

    if(!movementName.isNullOrBlank()) {
      parseOutput.movementName = movementName
    }
    if(!genre.isNullOrBlank()) {
      parseOutput.genre = genre
    }
    buffer.position = positionBeforeParsing
  }

  fun parseAtoms(buffer: ParsableByteArray, parentAtom: MetaAtom) {
    while(buffer.position < parentAtom.end) {
      val position = buffer.position
      // we can't read beyond the buffer size
      if(buffer.position >= buffer.data.size - 4) {
        break
      }
      val atomSize = buffer.readInt()

      /*
      val bytes = ByteBuffer.allocate(4)
      buffer.readBytes(bytes, 4)
      */
      val atomName = buffer.readString(4, Charsets.ISO_8859_1)
      val subAtom = MetaAtom(atomName, position, atomSize)
      extractMetaDataField(buffer, parentAtom, atomSize-8)


      if(atomSize > 0 && isAtomNameSupported(atomName) && subAtom.end <= parentAtom.end) {
        parseAtoms(buffer, subAtom)
      } else {
        buffer.position = parentAtom.end
      }
    }
  }

  private fun extractMetaDataField(
    buffer: ParsableByteArray,
    parentAtom: MetaAtom,
    size: Int,
  ) {
    // parentAtom is named and has a data field
    when (parentAtom.name) {
      nameField -> name = parseDataAtomString(buffer, size)
      titleField -> title = parseDataAtomString(buffer, size)
      genreField -> genre = parseDataAtomString(buffer, size)
      genreField2 -> genre = parseDataAtomString(buffer, size)
      movementNameField -> movementName = parseDataAtomString(buffer, size)
      movementIndexField -> movementIndex = parseDataAtomInt(buffer)?.toString()
      customField -> parseCustomField(buffer, size)
    }
  }




  private fun parseDataAtomInt(buffer: ParsableByteArray): Int? {
    parseFlags(buffer)
    var value = buffer.readInt().reverseBytes()
    return value
  }

  private fun parseCustomField(buffer: ParsableByteArray, size: Int) {
    parseFlags(buffer)
    val x = buffer.readString(size - 8)

    val y = "y"
  }

  private fun parseDataAtomString(buffer: ParsableByteArray, size:Int): String? {
    parseFlags(buffer)
    val value = buffer.readString(size - 8/*, Charsets.ISO_8859_1*/)
    return value
  }


  private fun parseFlags(buffer: ParsableByteArray): Int {
    val byteBuffer = ByteBuffer.allocate(4)
    buffer.readBytes(byteBuffer, 4)
    val byteArray = byteBuffer.array()

    // val version = bytesToInt(byteArray, 0, 1)
    // 0=uint8
    // 1=text
    // 21=uint8
    val flags = bytesToInt(byteArray, 1, 3)
    // skip null space
    buffer.position += 4
    return flags
  }

  private fun bytesToInt(byteArray: ByteArray, offset: Int, length: Int): Int {
    val bytes = byteArray.drop(offset).take(length)
    var value = 0
    for (b in bytes) {
      value = (value shl 8) + (b.toInt() and 0xFF)
    }
    return value
  }

  private fun isAtomNameSupported(atomName: String): Boolean {
    return atomName.all { it -> it.isLetterOrDigit() || it == '-' || it == '©'}
  }
}
