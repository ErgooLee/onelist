package io.github.ergoo.onelist.demo.home.horizontal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.github.ergoo.onelist.BindingViewHolder
import io.github.ergoo.onelist.ListBinder
import io.github.ergoo.onelist.demo.R
import io.github.ergoo.onelist.demo.home.VideoCardPalette
import io.github.ergoo.onelist.demo.databinding.HorizontalVideoLayoutBinding

class HorizontalVideoListBinder : ListBinder<HorizontalVideo, HorizontalVideoViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HorizontalVideoViewHolder {
        val binding =
            HorizontalVideoLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HorizontalVideoViewHolder(binding)
    }

    override fun convert(holder: HorizontalVideoViewHolder, data: HorizontalVideo) {
        holder.bind(data)
    }

    override fun convert(
        holder: HorizontalVideoViewHolder,
        data: HorizontalVideo,
        payloads: List<Any>
    ) {
        if (payloads.contains("like")) {
            holder.changeLike(data.liked)
        } else {
            convert(holder, data)
        }
    }

    override fun getViewType(): Int = R.layout.horizontal_video_layout

    override fun spanCount(): Int {
        return 3
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HorizontalVideo>() {
            override fun areItemsTheSame(
                oldItem: HorizontalVideo,
                newItem: HorizontalVideo
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: HorizontalVideo,
                newItem: HorizontalVideo
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: HorizontalVideo,
                newItem: HorizontalVideo
            ): Any? {
                if (oldItem.liked != newItem.liked) {
                    return "like"
                }
                return super.getChangePayload(oldItem, newItem)
            }
        }
    }
}

class HorizontalVideoViewHolder(binding: HorizontalVideoLayoutBinding) :
    BindingViewHolder<HorizontalVideoLayoutBinding>(binding) {

    fun bind(data: HorizontalVideo) {
        binding.titleTv.text = data.title
        binding.subtitleTv.text = data.subTitle
        changeLike(data.liked)
        binding.coverImageView.setBackgroundColor(
            VideoCardPalette.colorFor(binding.root.context, data.id)
        )
    }

    fun changeLike(like: Boolean) {
        if (like) {
            binding.likeImg.setImageResource(R.drawable.ic_favorite)
        } else {
            binding.likeImg.setImageResource(R.drawable.ic_favorite_no)
        }
    }
}

