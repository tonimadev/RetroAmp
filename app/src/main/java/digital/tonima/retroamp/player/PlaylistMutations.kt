package digital.tonima.retroamp.player

import digital.tonima.retroamp.core.model.Track

/**
 * Pure playlist-mutation logic extracted from [PlayerViewModel] so it can be
 * unit tested without a ViewModel, a coroutine scope, or a real PlayerManager.
 */

internal data class AddTracksResult(
    val updatedPlaylist: List<Track>,
    val newTracksToAdd: List<Track>,
    val hadDuplicates: Boolean
)

/**
 * Merges [incomingTracks] into [currentPlaylist], skipping anything already
 * present (by [Track.id]).
 *
 * [hadDuplicateSelection] flags that the caller's raw input (e.g. URIs picked
 * from the file picker) already contained duplicates *before* metadata was
 * extracted - that information is lost by the time we only have [Track]
 * objects (whose id is derived from the source URI), so it must be passed in
 * rather than re-derived here.
 */
internal fun mergeIncomingTracks(
    currentPlaylist: List<Track>,
    incomingTracks: List<Track>,
    hadDuplicateSelection: Boolean
): AddTracksResult {
    val currentIds = currentPlaylist.map { it.id }.toSet()
    val newTracksToAdd = incomingTracks.filter { it.id !in currentIds }.distinctBy { it.id }
    val hadDuplicates = hadDuplicateSelection || incomingTracks.size > newTracksToAdd.size
    val updatedPlaylist = if (newTracksToAdd.isNotEmpty()) {
        (currentPlaylist + newTracksToAdd).distinctBy { it.id }
    } else {
        currentPlaylist
    }
    return AddTracksResult(updatedPlaylist, newTracksToAdd, hadDuplicates)
}

internal data class RemoveTrackResult(
    val newPlaylist: List<Track>,
    val newCurrentTrack: Track?,
    val removedIndex: Int
)

/**
 * Removes the track with [trackId] from [currentPlaylist]. Returns `null` if
 * no such track exists (nothing to do).
 *
 * When the removed track was the current one, the new current track becomes
 * whatever now sits at the same index - or the new last item if the removed
 * track was the last one - so playback can hand off to "the next track" (or
 * `null` if the playlist is now empty).
 */
internal fun removeTrackFromPlaylist(
    currentPlaylist: List<Track>,
    currentTrack: Track?,
    trackId: String
): RemoveTrackResult? {
    val index = currentPlaylist.indexOfFirst { it.id == trackId }
    if (index == -1) return null

    val newList = currentPlaylist.toMutableList().apply { removeAt(index) }
    val newCurrentTrack = if (currentTrack?.id == trackId) {
        if (newList.isNotEmpty()) newList[index.coerceAtMost(newList.size - 1)] else null
    } else {
        currentTrack
    }
    return RemoveTrackResult(newList, newCurrentTrack, index)
}
