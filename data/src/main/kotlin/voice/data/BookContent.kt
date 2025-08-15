package voice.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import voice.common.BookId
import java.io.File
import java.time.Instant

@Entity(tableName = "content2")
data class BookContent(
  @PrimaryKey
  val id: BookId,
  val playbackSpeed: Float,
  val skipSilence: Boolean,
  val isActive: Boolean,
  val lastPlayedAt: Instant,
  val author: String?,
  val name: String,
  val addedAt: Instant,
  val chapters: List<ChapterId>,
  val currentChapter: ChapterId,
  val positionInChapter: Long,
  val cover: File?,
  @ColumnInfo(defaultValue = "0")
  val gain: Float,
  @ColumnInfo(defaultValue = "")
  val genre: String?,
  @ColumnInfo(defaultValue = "")
  val narrator: String?,
  @ColumnInfo(defaultValue = "")
  val movementName: String?,
  @ColumnInfo(defaultValue = "")
  val part: String?,
  ) {

  @Ignore
  val currentChapterIndex = chapters.indexOf(currentChapter)

  init {
    require(currentChapter in chapters && positionInChapter >= 0) {
      "invalid data in $this"
    }
  }
}
