@file:Suppress("DEPRECATION")

package voice.playback.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import voice.playback.session.PlaybackService

@ContributesGraphExtension(
  scope = PlaybackScope::class,
)
@PlaybackScope
interface PlaybackGraph {

  fun inject(target: PlaybackService)

  @ContributesTo(AppScope::class)
  @ContributesGraphExtension.Factory(Factory::class)
  interface Factory {
    fun create(@Provides playbackService: PlaybackService): PlaybackGraph
  }

  @ContributesTo(AppScope::class)
  interface Provider {
    val playbackGraphFactory: Factory
  }
}
