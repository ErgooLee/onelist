package io.github.ergoo.onelist.demo

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.ergoo.onelist.demo.home.HomeFragment

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return HomeFragment.newInstance(1)
            1 -> return BaseChannelFragment.newInstance(2)
            2 -> return BaseChannelFragment.newInstance(3)
            3 -> return BaseChannelFragment.newInstance(4)
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}

