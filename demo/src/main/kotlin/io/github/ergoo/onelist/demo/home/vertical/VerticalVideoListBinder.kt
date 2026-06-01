package io.github.ergoo.onelist.demo.home.vertical

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.github.ergoo.onelist.BindingViewHolder
import io.github.ergoo.onelist.ListBinder
import io.github.ergoo.onelist.demo.R
import io.github.ergoo.onelist.demo.home.VideoCardPalette
import io.github.ergoo.onelist.demo.databinding.VerticalVideoLayoutBinding

class VerticalVideoListBinder : ListBinder<VerticalVideo, VerticalVideoViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VerticalVideoViewHolder {
        val binding =
            VerticalVideoLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VerticalVideoViewHolder(binding)
    }

    override fun convert(holder: VerticalVideoViewHolder, data: VerticalVideo) {
        holder.bind(data)
    }

    override fun spanCount(): Int {
        return 2
    }

    override fun getViewType(): Int = R.layout.vertical_video_layout

    override fun convert(
        holder: VerticalVideoViewHolder,
        data: VerticalVideo,
        payloads: List<Any>
    ) {
        if (payloads.contains("like")) {
            holder.setLikeStatus(data.liked)
        } else {
            convert(holder, data)
        }
    }



    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VerticalVideo>() {
            override fun areItemsTheSame(oldItem: VerticalVideo, newItem: VerticalVideo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: VerticalVideo,
                newItem: VerticalVideo
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: VerticalVideo, newItem: VerticalVideo): Any? {
                if (oldItem.liked != newItem.liked) {
                    return "like"
                }
                return super.getChangePayload(oldItem, newItem)
            }
        }
    }
}

class VerticalVideoViewHolder(binding: VerticalVideoLayoutBinding) :
    BindingViewHolder<VerticalVideoLayoutBinding>(binding) {

    fun bind(data: VerticalVideo) {
        binding.titleTv.text = data.title
        binding.subtitleTv.text = data.subTitle
        setLikeStatus(data.liked)
        binding.coverImageView.setBackgroundColor(
            VideoCardPalette.colorFor(binding.root.context, data.id)
        )
    }

    fun setLikeStatus(like: Boolean) {
        if (like) {
            binding.likeImg.setImageResource(R.drawable.ic_favorite)
        } else {
            binding.likeImg.setImageResource(R.drawable.ic_favorite_no)
        }
    }
}

