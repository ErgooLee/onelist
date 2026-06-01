package io.github.ergoo.onelist

/**
 * Calculates evenly-distributed start/end margins for items in a grid layout
 * so that every item has the same width and the spacing between items and edges
 * is visually balanced.
 *
 * Supports both LTR and RTL layouts: margins are computed in logical
 * (start/end) order and can be converted to physical (left/right) via [isRtl].
 *
 * Given [marginStart], [marginEnd], [itemSpace], [parentWidth], and grid column
 * count ([managerSpanCount] / [spanCount]), this class pre-computes:
 * - [materialWidth] / [materialHeight] — the resulting item dimensions.
 * - Per-column margin pairs accessible via [getMargin].
 *
 * @param marginStart          the horizontal margin on the start edge of the grid (px).
 * @param marginEnd            the horizontal margin on the end edge of the grid (px).
 * @param itemSpace            the horizontal space between adjacent items (px).
 * @param parentWidth          the total width of the parent container (px).
 * @param managerSpanCount     the span count of the LayoutManager.
 * @param materialWidthHeightRatio  the desired width-to-height ratio for items.
 * @param spanCount            the span size of each item (defaults to 1).
 * @param isRtl                whether the layout direction is right-to-left.
 */
data class SpanItemMarginHelper(
    val marginStart: Int,
    val marginEnd: Int,
    val itemSpace: Int,
    val parentWidth: Int,
    val managerSpanCount: Int,
    val materialWidthHeightRatio: Float,
    val spanCount: Int = 1,
    val isRtl: Boolean = false,
) {

    /** Number of items that fit in a single row. */
    val countPerLine = managerSpanCount / spanCount

    private val allSpace = marginStart + marginEnd + itemSpace * (countPerLine - 1).coerceAtLeast(0)
    private val avgSpace = if (countPerLine > 0) allSpace.toFloat() / countPerLine else 0f

    /** Computed item width after subtracting all horizontal spacing. */
    val materialWidth = if (countPerLine > 0) (parentWidth - allSpace) / countPerLine else 0

    /** Computed item height derived from [materialWidth] × [materialWidthHeightRatio]. */
    val materialHeight = (materialWidth * materialWidthHeightRatio).toInt()

    /**
     * Pre-computed (left, right) margin pair for each column index.
     *
     * In LTR mode the start margin maps to left; in RTL mode the list is
     * mirrored so column 0 starts from the right edge.
     */
    private val margins = mutableListOf<Pair<Int, Int>>()

    init {
        if (countPerLine > 0) {
            // Compute in logical (start → end) order first.
            val logical = mutableListOf<Pair<Int, Int>>()
            var start = marginStart.toFloat()
            var end: Float
            (0 until countPerLine).forEach { _ ->
                end = avgSpace - start
                logical.add(Pair(start.toInt(), end.toInt()))
                start = itemSpace - end
            }

            if (isRtl) {
                // Mirror: column 0 is the rightmost; swap start/end per entry.
                logical.asReversed().forEach { (s, e) ->
                    margins.add(Pair(e, s))
                }
            } else {
                margins.addAll(logical)
            }
        }
    }

    /**
     * Returns the (left, right) margin pair for the given column [index].
     *
     * In RTL layouts the returned values are already mirrored so they can be
     * applied directly as left/right.
     *
     * @param index the zero-based column index, i.e. `adapterPosition % countPerLine`.
     * @throws IndexOutOfBoundsException if [index] is outside `0 until countPerLine`.
     */
    fun getMargin(index: Int): Pair<Int, Int> {
        if (index !in margins.indices) {
            throw IndexOutOfBoundsException("column index: $index, countPerLine: $countPerLine")
        }
        return margins[index]
    }

}