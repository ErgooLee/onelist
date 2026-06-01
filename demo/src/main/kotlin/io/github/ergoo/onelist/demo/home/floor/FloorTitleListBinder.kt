package io.github.ergoo.onelist.demo.home.floor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.github.ergoo.onelist.BindingViewHolder
import io.github.ergoo.onelist.FullSpan
import io.github.ergoo.onelist.ListBinder
import io.github.ergoo.onelist.demo.R
import io.github.ergoo.onelist.demo.databinding.FloorTitleLayoutBinding

class FloorTitleListBinder : ListBinder<FloorTitle, FloorTitleViewHolder>(), FullSpan {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FloorTitleViewHolder {
        val binding =
            FloorTitleLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FloorTitleViewHolder(binding)
    }

    override fun convert(holder: FloorTitleViewHolder, data: FloorTitle) {
        holder.bind(data)
    }

    override fun getViewType(): Int = R.layout.floor_title_layout

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FloorTitle>() {
            override fun areItemsTheSame(oldItem: FloorTitle, newItem: FloorTitle): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FloorTitle, newItem: FloorTitle): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class FloorTitleViewHolder(binding: FloorTitleLayoutBinding) :
    BindingViewHolder<FloorTitleLayoutBinding>(binding) {

    fun bind(data: FloorTitle) {
        binding.titleTv.text = data.title
    }
}

