package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * An adapter that displays **zero or one** item based on a [status] value.
 *
 * Whether the single item is visible is determined by [isValid]: when the
 * status transitions between valid and invalid the adapter dispatches the
 * appropriate `notifyItemInserted` / `notifyItemRemoved` / `notifyItemChanged`.
 *
 * Typical use cases include empty-state placeholders, loading indicators,
 * or error banners that appear conditionally.
 *
 * @param T  the status type that drives visibility and binding.
 * @param VH the type of [RecyclerView.ViewHolder].
 * @param initValue the initial status value.
 */
abstract class OneItemAdapter<T : Any, VH : RecyclerView.ViewHolder>(initValue: T) :
    OneListAdapter<T, VH>() {

    /**
     * The current status value. Updating it automatically notifies the
     * RecyclerView of insertion, removal, or change as appropriate.
     */
    open var status: T = initValue
        set(value) {
            if (field != value) {
                val oldValid = isValid(field)
                val newValid = isValid(value)
                field = value
                if (oldValid && !newValid) {
                    notifyItemRemoved(0)
                } else if (newValid && !oldValid) {
                    notifyItemInserted(0)
                } else if (oldValid) {
                    notifyItemChanged(0)
                }
            }
        }

    /**
     * Returns `1` when [status] is valid (the item is visible), `0` otherwise.
     */
    override fun getItemCount(): Int {
        return if (isValid(status)) 1 else 0
    }

    /** Always returns the current [status] (there is at most one item). */
    override fun getItem(position: Int): T {
        return status
    }

    /**
     * Determines whether the given [status] should produce a visible item.
     *
     */
    abstract fun isValid(status: T): Boolean

    /** Delegates to [onCreateViewHolderByStatus] with the current [status]. */
    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): VH {
        return onCreateViewHolderByStatus(parent, status)
    }

    /** Delegates to [onBindViewHolderByStatus]. */
    override fun onBindViewHolder(holder: VH, position: Int, item: T) {
        onBindViewHolderByStatus(holder, item)
    }

    /** Delegates to [getStateViewType]. */
    override fun getItemViewType(position: Int, item: T): Int {
        return getStateViewType(item)
    }

    /**
     * Creates a new [VH] for the given [status].
     *
     * @param parent the parent [ViewGroup].
     * @param status the current status driving the view's appearance.
     */
    abstract fun onCreateViewHolderByStatus(parent: ViewGroup, status: T): VH

    /**
     * Binds [status] to [holder].
     *
     * @param holder the ViewHolder to bind.
     * @param status the current status value.
     */
    abstract fun onBindViewHolderByStatus(holder: VH, status: T)

    /**
     * Returns the view type for the given [status]. Defaults to `0`.
     * Override if different status values require different layouts.
     */
    open fun getStateViewType(status: T): Int = 0

}

