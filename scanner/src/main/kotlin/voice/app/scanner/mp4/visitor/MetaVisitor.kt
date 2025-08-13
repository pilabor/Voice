package voice.app.scanner.mp4.visitor

import androidx.media3.common.util.ParsableByteArray
import okio.ByteString.Companion.toByteString
import voice.app.scanner.mp4.Mp4ChpaterExtractorOutput
import java.nio.ByteBuffer
import kotlin.reflect.typeOf

data class MetaAtom(val name:String,val position:Int, val size: Int) {
  val end: Int
    get() = position + size
  val children = mutableListOf<MetaAtom>()
}

class MetaVisitor : AtomVisitor {
  override val path: List<String> = listOf("moov", "udta", "meta")
  var name: String? = null

  override fun visit(
    buffer: ParsableByteArray,
    parseOutput: Mp4ChpaterExtractorOutput,
  ) {

    val metaVersionAndFlags = buffer.readString(4)
    val parentAtom = MetaAtom("meta", buffer.position, buffer.data.size)
    parseAtoms(buffer, parentAtom)
  }

  fun parseAtoms(buffer: ParsableByteArray, parentAtom: MetaAtom) {
    while(buffer.position < parentAtom.end) {
      val position = buffer.position
      val atomSize = buffer.readInt()

      /*
      val bytes = ByteBuffer.allocate(4)
      buffer.readBytes(bytes, 4)
      */
      val atomName = buffer.readString(4, Charsets.ISO_8859_1)
      val subAtom = MetaAtom(atomName, position, atomSize)

      if(parentAtom.name == "©nam") {
        name = parseDataAtomString(buffer, atomSize-8)
      }

      if(atomSize > 0 && isAtomNameSupported(atomName) && subAtom.end <= parentAtom.end) {
        parseAtoms(buffer, subAtom)
      } else {
        buffer.position = parentAtom.end
      }
    }
  }

  private fun parseDataAtomString(buffer: ParsableByteArray, size:Int): String? {
    return buffer.readString(size, Charsets.ISO_8859_1)
  }

  private fun isAtomNameSupported(atomName: String): Boolean {
    return atomName.all { it -> it.isLetterOrDigit() || it == '-' || it == '©'}
  }
}
