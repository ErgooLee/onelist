package io.github.ergoo.onelist.demo

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView

open class BaseChannelFragment : Fragment() {

    protected val pageIndex: Int
        get() = arguments?.getInt(ARG_PAGE_INDEX, 1) ?: 1

    protected val baseChannelViewModel by lazy {
        ViewModelProvider(this)[BaseChannelViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(LifecycleLogObserver(TAG, "fragment page=$pageIndex"))
    }

    companion object {
        private const val TAG = "BaseChannelFragment"
        protected const val ARG_PAGE_INDEX = "arg_page_index"

        fun newInstance(pageIndex: Int): BaseChannelFragment = BaseChannelFragment().apply {
            arguments = Bundle().apply { putInt(ARG_PAGE_INDEX, pageIndex) }
        }
    }
}

class BaseChannelViewModel : ViewModel() {
    val pool = RecyclerView.RecycledViewPool()
}
