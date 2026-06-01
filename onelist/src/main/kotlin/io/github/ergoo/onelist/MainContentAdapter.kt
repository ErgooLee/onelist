package io.github.ergoo.onelist

/**
 * Marker interface for the primary content adapter inside a [androidx.recyclerview.widget.ConcatAdapter].
 *
 * Consumers such as [EmptyContentAdapter] and [io.github.ergoo.onelist.more.LoadMoreAdapter]
 * use this marker to auto-discover the main data adapter instead of relying on fixed
 * adapter ordering.
 *
 * Notes:
 * - Mark exactly one adapter in a ConcatAdapter as [MainContentAdapter].
 * - For [io.github.ergoo.onelist.more.LoadMoreAdapter], the marked adapter must also be
 *   a [OneListAdapter] so preload observation can subscribe to item attach events.
 */
interface MainContentAdapter