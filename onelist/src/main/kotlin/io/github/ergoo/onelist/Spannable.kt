package io.github.ergoo.onelist

/**
 * Interface for adapters that provide per-item span control in a
 * [OneListGridLayoutManager].
 *
 * Implemented by [OneListAdapter] with default values (single-span, no full-span).
 * Override the methods in subclasses or [ListBinder]s to customise span behaviour.
 *
 * @see OneListGridLayoutManager.FullSpanSizeLookup
 * @see FullSpan
 */
interface Spannable {

    /**
     * Returns `true` if items with the given [itemType] should span all columns.
     *
     * @param itemType the view type returned by `getItemViewType`.
     */
    fun isFullSpanByViewType(itemType: Int): Boolean

    /**
     * Returns `true` if the item at [position] should span all columns.
     *
     * Default implementation in [OneListAdapter] delegates to
     * [isFullSpanByViewType].
     *
     * @param position the adapter position of the item.
     */
    fun isFullSpanItemByPosition(position: Int): Boolean

    /**
     * Returns the number of grid columns the item at [position] should span.
     *
     * Only called for items where [isFullSpanByViewType] returns `false`.
     * Defaults to `1` in [OneListAdapter].
     *
     * @param position the adapter position of the item.
     */
    fun spanCount(position: Int): Int
}