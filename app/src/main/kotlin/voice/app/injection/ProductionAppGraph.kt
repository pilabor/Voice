package voice.app.injection

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.playback.misc.VolumeGain

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
  isExtendable = true,
)
interface ProductionAppGraph : AppGraph {
  val volumeGain: VolumeGain
  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): ProductionAppGraph
  }
}
