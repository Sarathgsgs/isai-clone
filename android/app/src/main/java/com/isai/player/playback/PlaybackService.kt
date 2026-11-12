package com.isai.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.isai.player.MainActivity
import com.isai.player.R
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.playback.audio.AudioEffectsManager
import com.isai.player.playback.car.ClientTrustLevel
import com.isai.player.playback.car.MediaBrowseTree
import com.isai.player.playback.car.PackageValidator
import com.isai.player.playback.car.VoiceSearchResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    companion object {
        const val CUSTOM_ACTION_TOGGLE_FAVORITE = "com.isai.player.action.TOGGLE_FAVORITE"
    }

    @Inject
    lateinit var queueStore: QueueStore

    @Inject
    lateinit var playHistoryTracker: PlayHistoryTracker

    @Inject
    lateinit var sleepTimer: SleepTimer

    @Inject
    lateinit var audioEffectsManager: AudioEffectsManager

    @Inject
    lateinit var packageValidator: PackageValidator

    @Inject
    lateinit var mediaBrowseTree: MediaBrowseTree

    @Inject
    lateinit var voiceSearchResolver: VoiceSearchResolver

    @Inject
    lateinit var favoritesRepository: FavoritesRepository

    private var exoPlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val librarySessionCallback = object : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val trustLevel = packageValidator.getTrustLevel(controller.packageName)

            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build()

            val playerCommands = if (trustLevel == ClientTrustLevel.UNTRUSTED_READ_ONLY) {
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_METADATA
                    )
                    .build()
            } else {
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            }

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            controller: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(mediaBrowseTree.getRootItem(), params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            controller: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val children = mediaBrowseTree.getChildren(parentId, page, pageSize)
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
                } catch (e: Exception) {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            controller: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                try {
                    val tracks = mediaBrowseTree.resolveTracksForPlayback(mediaId)
                    if (tracks.isNotEmpty()) {
                        val mediaItem = MediaItemMapper.toMediaItem(tracks.first())
                        future.set(LibraryResult.ofItem(mediaItem, null))
                    } else {
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                } catch (e: Exception) {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            controller: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val tracks = voiceSearchResolver.resolveSearch(query)
                session.notifySearchResultChanged(controller, query, tracks.size, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            controller: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val tracks = voiceSearchResolver.resolveSearch(query)
                    val mediaItems = tracks.map { MediaItemMapper.toMediaItem(it) }
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                } catch (e: Exception) {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            serviceScope.launch {
                try {
                    val resolvedList = mutableListOf<MediaItem>()
                    for (item in mediaItems) {
                        val tracks = mediaBrowseTree.resolveTracksForPlayback(item.mediaId)
                        if (tracks.isNotEmpty()) {
                            resolvedList.addAll(tracks.map { MediaItemMapper.toMediaItem(it) })
                        } else {
                            resolvedList.add(item)
                        }
                    }
                    future.set(resolvedList)
                } catch (e: Exception) {
                    future.set(mediaItems)
                }
            }
            return future
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_ACTION_TOGGLE_FAVORITE) {
                val track = session.player.currentMediaItem?.let { MediaItemMapper.toTrack(it) }
                if (track != null) {
                    serviceScope.launch {
                        favoritesRepository.toggleFavorite(track.id)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        // Configure audio attributes for music playback with automatic focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Build ExoPlayer with gapless support, audio focus & becoming noisy auto-pause
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        exoPlayer = player
        audioEffectsManager.attachPlayer(player)

        // Activity launch PendingIntent when user taps notification
        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build MediaLibrarySession
        val session = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        mediaLibrarySession = session

        // Configure system notification provider
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NotificationHelper.CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .setNotificationId(NotificationHelper.NOTIFICATION_ID)
            .build()

        setMediaNotificationProvider(notificationProvider)

        // Player event listener for play history, queue sync, and corrupt track auto-skip
        player.addListener(object : Player.Listener {
            private var consecutiveErrorCount = 0

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                consecutiveErrorCount = 0
                val track = mediaItem?.let(MediaItemMapper::toTrack)
                playHistoryTracker.onTrackChanged(track)
            }

            override fun onPlayerError(error: PlaybackException) {
                consecutiveErrorCount++
                val queueSize = player.mediaItemCount
                if (queueSize > 1 && consecutiveErrorCount < queueSize) {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    } else {
                        player.seekToDefaultPosition(0)
                    }
                    player.prepare()
                    player.play()
                } else {
                    player.stop()
                    consecutiveErrorCount = 0
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    sleepTimer.onTrackEnded {
                        player.pause()
                    }
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffectsManager.onAudioSessionIdChanged(audioSessionId)
            }
        })

        // Restore previous queue on cold start
        serviceScope.launch {
            val restoredQueue = queueStore.restoreQueue()
            if (restoredQueue.isNotEmpty()) {
                val mediaItems = restoredQueue.map(MediaItemMapper::toMediaItem)
                player.setMediaItems(mediaItems)
                player.prepare()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        audioEffectsManager.releaseEffects()
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
