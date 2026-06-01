package io.github.ergoo.onelist

/**
 * Marker interface indicating that the implementing component should occupy
 * the full width of the parent [androidx.recyclerview.widget.RecyclerView].
 *
 * Can be implemented by:
 * - A [ListBinder] within [MergeAdapter] / [DifferMergeAdapter].
 * - An adapter used inside [androidx.recyclerview.widget.ConcatAdapter].
 *
 * When used with [OneListGridLayoutManager], items from a component
 * implementing this interface will automatically span all columns.
 * For [androidx.recyclerview.widget.StaggeredGridLayoutManager], the holder's
 * `isFullSpan` flag is set to `true` in
 * [OneListAdapter.onViewAttachedToWindow].
 *
 * @see MergeAdapter.isFullSpanByViewType
 * @see Spannable
 */
interface FullSpan