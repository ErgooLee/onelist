package io.github.ergoo.onelist

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * A [androidx.recyclerview.widget.RecyclerView.ViewHolder] that holds a [ViewBinding] instance, providing convenient
 * access to view-bound layouts without manual `findViewById` calls.
 *
 * @param VB the type of [ViewBinding] associated with this ViewHolder.
 * @param binding the ViewBinding instance for the item layout.
 */
open class BindingViewHolder<VB : ViewBinding>(val binding: VB) :
    RecyclerView.ViewHolder(binding.root)
