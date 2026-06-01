package io.github.ergoo.onelist

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A [GridLayoutManager] that automatically resolves span sizes for adapters
 * implementing [Spannable] or [FullSpan].
 *
 * Works transparently with both plain adapters and [ConcatAdapter]:
 * - [FullSpan] adapters / binders always span all columns.
 * - [Spannable] adapters delegate to [Spannable.isFullSpanByViewType] and
 *   [Spannable.spanCount] for per-item control.
 * - Other adapters fall through to the user-supplied [SpanSizeLookup] (if any).
 *
 * Usage: simply replace [GridLayoutManager] with [OneListGridLayoutManager].
 * Any custom [SpanSizeLookup] set via [setSpanSizeLookup] is preserved as a
 * fallback for unrecognised adapter types.
 */
open class OneListGridLayoutManager : GridLayoutManager {

    private var pendingOriginalSpanSizeLookup: SpanSizeLookup? = null

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, spanCount: Int) : super(context, spanCount)

    constructor(
        context: Context, spanCount: Int,
        @RecyclerView.Orientation orientation: Int, reverseLayout: Boolean
    ) : super(context, spanCount, orientation, reverseLayout)

    /** Internal span-size lookup that intercepts queries for [FullSpan] / [Spannable]. */
    private var _oneListSpanSizeLookup: FullSpanSizeLookup? = null

    val oneListSpanSizeLookup: FullSpanSizeLookup
        get() = checkNotNull(_oneListSpanSizeLookup) {
            "OneListGridLayoutManager has not finished initialization yet."
        }

    init {
        val lookup = FullSpanSizeLookup().also {
            it.originalSpanSizeLookup = pendingOriginalSpanSizeLookup ?: spanSizeLookup
            it.spanCount = spanCount
        }
        _oneListSpanSizeLookup = lookup

        super.setSpanSizeLookup(lookup)
    }

    /** Updates the internal adapter reference when the RecyclerView swaps adapters. */
    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?
    ) {
        _oneListSpanSizeLookup?.adapter = newAdapter
    }

    /**
     * Stores the caller's [SpanSizeLookup] as a fallback; the actual lookup
     * installed on the super class is always [oneListSpanSizeLookup].
     */
    override fun setSpanSizeLookup(spanSizeLookup: SpanSizeLookup?) {
        pendingOriginalSpanSizeLookup = spanSizeLookup
        _oneListSpanSizeLookup?.originalSpanSizeLookup = spanSizeLookup
    }

    override fun setSpanCount(spanCount: Int) {
        super.setSpanCount(spanCount)
        _oneListSpanSizeLookup?.spanCount = spanCount
    }

    /**
     * [SpanSizeLookup] implementation that checks adapters for [FullSpan] or
     * [Spannable] before falling back to [originalSpanSizeLookup].
     */
    class FullSpanSizeLookup : SpanSizeLookup() {

        /** Current grid column count; kept in sync by [OneListGridLayoutManager.setSpanCount]. */
        var spanCount: Int = 1

        /** The adapter currently set on the RecyclerView. */
        var adapter: RecyclerView.Adapter<*>? = null

        /** User-supplied fallback lookup for adapters that are neither [FullSpan] nor [Spannable]. */
        var originalSpanSizeLookup: SpanSizeLookup? = null

        override fun getSpanSize(position: Int): Int {
            val adapter = adapter ?: return 1

            if (adapter is ConcatAdapter) {
                val pair = adapter.getWrappedAdapterAndPosition(position)
                return resolveSpanSize(pair.first, pair.second, position)
            }

            return resolveSpanSize(adapter, position, position)
        }

        /**
         * Resolves the span size for an [adapter] at [localPosition].
         *
         * @param adapter the (possibly wrapped) adapter to inspect.
         * @param localPosition the position relative to [adapter].
         * @param globalPosition the position in the top-level adapter,
         *        used as fallback for [originalSpanSizeLookup].
         */
        private fun resolveSpanSize(
            adapter: RecyclerView.Adapter<*>,
            localPosition: Int,
            globalPosition: Int
        ): Int {
            return when (adapter) {
                is FullSpan -> spanCount

                is Spannable -> {
                    val type = adapter.getItemViewType(localPosition)
                    if (adapter.isFullSpanByViewType(type)) {
                        spanCount
                    } else {
                        adapter.spanCount(localPosition)
                    }
                }

                else -> originalSpanSizeLookup?.getSpanSize(globalPosition) ?: 1
            }
        }
    }
}