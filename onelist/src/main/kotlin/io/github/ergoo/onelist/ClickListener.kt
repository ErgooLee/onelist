package io.github.ergoo.onelist

import android.view.View

/**
 * Simplified click listener that receives only the data item and the clicked view.
 *
 * @param T the type of data associated with the clicked item.
 */
interface SimpleClickListener<T> {
    fun onClick(data: T, view: View)
}

/**
 * Simplified long-click listener that receives only the data item and the clicked view.
 *
 * @param T the type of data associated with the clicked item.
 */
interface SimpleLongClickListener<T> {
    fun onLongClick(data: T, view: View): Boolean
}

/**
 * Full click listener that additionally provides the ViewHolder for advanced use cases
 * (e.g. accessing adapter position or child views).
 *
 * @param T  the type of data associated with the clicked item.
 * @param VH the type of ViewHolder that was clicked.
 */
interface ClickListener<T, VH> {
    fun onClick(data: T, view: View, holder: VH)
}

/**
 * Adapts a [SimpleClickListener] to the [ClickListener] interface by discarding the
 * ViewHolder parameter. This allows simpler lambdas to be used where a full
 * [ClickListener] is expected.
 *
 * @param T  the type of data associated with the clicked item.
 * @param VH the type of ViewHolder (unused, forwarded for type compatibility).
 */
class ClickListenerWrapper<T, VH>(private val clickListener: SimpleClickListener<T>) :
    ClickListener<T, VH> {
    override fun onClick(data: T, view: View, holder: VH) {
        clickListener.onClick(data, view)
    }
}

/**
 * Full long-click listener that additionally provides the ViewHolder for advanced use cases.
 *
 * @param T  the type of data associated with the clicked item.
 * @param VH the type of ViewHolder that was long-clicked.
 */
interface LongClickListener<T, VH> {
    fun onLongClick(data: T, view: View, holder: VH): Boolean
}

/**
 * Adapts a [SimpleLongClickListener] to the [LongClickListener] interface by discarding the
 * ViewHolder parameter. This allows simpler lambdas to be used where a full
 * [LongClickListener] is expected.
 *
 * @param T  the type of data associated with the clicked item.
 * @param VH the type of ViewHolder (unused, forwarded for type compatibility).
 */
class LongClickListenerWrapper<T, VH>(private val clickListener: SimpleLongClickListener<T>) :
    LongClickListener<T, VH> {

    override fun onLongClick(data: T, view: View, holder: VH): Boolean {
        return clickListener.onLongClick(data, view)
    }

}