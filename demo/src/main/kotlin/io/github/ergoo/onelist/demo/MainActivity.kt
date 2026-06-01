package io.github.ergoo.onelist.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import io.github.ergoo.onelist.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var tabLayoutMediator: TabLayoutMediator? = null

    private val tabTitles = intArrayOf(
        R.string.tab_home,
        R.string.tab_discover,
        R.string.tab_message,
        R.string.tab_profile,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)

        tabLayoutMediator = TabLayoutMediator(
            binding.tabLayout, binding.viewPager, true, false
        ) { tab, position ->
            tab.setText(tabTitles[position])
        }.also { it.attach() }

        // ViewPager2 restores its own current item automatically via instance state,
        // but we must wait until the adapter is attached before restoring manually
        // if we ever need to override it (e.g., deep-link to a specific page).
        savedInstanceState?.getInt(KEY_CURRENT_ITEM, 0)?.let { page ->
            binding.viewPager.setCurrentItem(page, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_ITEM, binding.viewPager.currentItem)
    }

    override fun onDestroy() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        super.onDestroy()
    }

    companion object {
        private const val KEY_CURRENT_ITEM = "key_current_item"
    }
}

