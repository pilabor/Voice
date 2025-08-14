package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import dev.zacsweers.metro.Inject
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput

data class MetaAtom(val name:String,val position:Int, val size: Int) {
  val end: Int
    get() = position + size
  val children = mutableListOf<MetaAtom>()
}


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
    size: Int
  ) {
    when (parentAtom.name) {
      nameField -> name = parseDataAtomString(buffer, size)
      titleField -> title = parseDataAtomString(buffer, size)
      genreField -> genre = parseDataAtomString(buffer, size)
      genreField2 -> genre = parseDataAtomString(buffer, size)
      movementNameField -> movementName = parseDataAtomString(buffer, size)
    }
  }

  private fun parseDataAtomString(buffer: ParsableByteArray, size:Int): String? {
    return buffer.readString(size, Charsets.ISO_8859_1).trim()
  }

  private fun isAtomNameSupported(atomName: String): Boolean {
    return atomName.all { it -> it.isLetterOrDigit() || it == '-' || it == '©'}
  }
}
