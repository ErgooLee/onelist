package io.github.ergoo.onelist.demo.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import io.github.ergoo.onelist.ClickListener
import io.github.ergoo.onelist.EmptyContentAdapter
import io.github.ergoo.onelist.SimpleClickListener
import io.github.ergoo.onelist.demo.BaseChannelFragment
import io.github.ergoo.onelist.demo.LoadStateView
import io.github.ergoo.onelist.demo.R
import io.github.ergoo.onelist.demo.databinding.FragmentChannelHomeBinding
import io.github.ergoo.onelist.demo.home.horizontal.HorizontalVideo
import io.github.ergoo.onelist.demo.home.horizontal.HorizontalVideoViewHolder
import io.github.ergoo.onelist.demo.home.vertical.VerticalVideo
import io.github.ergoo.onelist.demo.home.vertical.VerticalVideoViewHolder
import io.github.ergoo.onelist.more.BottomLoadMoreAdapter
import io.github.ergoo.onelist.more.LoadState
import io.github.ergoo.onelist.more.OneListBottomLoadMore
import io.github.ergoo.onelist.refresh.OneListRefreshView
import io.github.ergoo.onelist.refresh.RefreshAdapter
import io.github.ergoo.onelist.refresh.RefreshState
import io.github.ergoo.onelist.setClickListener
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeFragment : BaseChannelFragment() {

    private var _binding: FragmentChannelHomeBinding? = null

    private val binding get() = _binding!!

    private val homeChannelViewModel by lazy {
        ViewModelProvider(this)[HomeChanelViewModel::class.java]
    }

    private val loadStateView by lazy {
        LoadStateView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChannelHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        if (homeChannelViewModel.items.isEmpty()) {
            homeChannelViewModel.refresh()
        }
    }

    private fun setupList() {

        val context = requireContext()
        val recyclerView = binding.list

        recyclerView.addItemDecoration(SpaceItemDecoration())

        val contentAdapter = HomeContentAdapter()
        val refreshAdapter = RefreshAdapter(OneListRefreshView(context))
        val loadMoreAdapter = BottomLoadMoreAdapter(OneListBottomLoadMore(context))
        val emptyAdapter = EmptyContentAdapter(loadStateView)

        val config = ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build()

        recyclerView.adapter = ConcatAdapter(
            config,
            refreshAdapter,
            contentAdapter,
            emptyAdapter,
            loadMoreAdapter,
        )

        loadStateView.setup()

        contentAdapter.setup()

        refreshAdapter.setup()

        loadMoreAdapter.setup { refreshAdapter.refreshing }


    }

    private fun LoadStateView.setup() {
        homeChannelViewModel.events.filter {
            homeChannelViewModel.items.isEmpty()
        }.onEach { event ->
            when (event) {
                is ChannelUiEvent.RefreshFinished -> {
                    if (!event.success) {
                        showError(event.error?.message) {
                            homeChannelViewModel.refresh()
                        }
                    }
                }

                ChannelUiEvent.StartRefreshing -> {
                    showLoading()
                }

                else -> {
                    // no-op
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun HomeContentAdapter.setup() {
        homeChannelViewModel.uiState.onEach { state ->
            Log.i(TAG, "new content ${state.rows.size}")
            submitList(state.rows)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        verticalVideoBinder.setClickListener(object : SimpleClickListener<VerticalVideo> {
            override fun onClick(
                data: VerticalVideo,
                view: View
            ) {
                Toast.makeText(context, "click video ${data.title}", Toast.LENGTH_LONG).show()
            }
        })

        horizontalVideoListBinder.setClickListener(object : SimpleClickListener<HorizontalVideo> {
            override fun onClick(
                data: HorizontalVideo,
                view: View
            ) {
                Toast.makeText(context, "click video ${data.title}", Toast.LENGTH_LONG).show()
            }
        })

        verticalVideoBinder.addOnItemChildClickListener(
            R.id.like_img,
            object : ClickListener<VerticalVideo, VerticalVideoViewHolder> {
                override fun onClick(
                    data: VerticalVideo,
                    view: View,
                    holder: VerticalVideoViewHolder
                ) {
                    homeChannelViewModel.like(data.id, !data.liked)
                }
            })

        horizontalVideoListBinder.addOnItemChildClickListener(
            R.id.like_img,
            object : ClickListener<HorizontalVideo, HorizontalVideoViewHolder> {
                override fun onClick(
                    data: HorizontalVideo,
                    view: View,
                    holder: HorizontalVideoViewHolder
                ) {
                    homeChannelViewModel.like(data.id, !data.liked)
                }
            })
    }

    private fun RefreshAdapter.setup() {

        allowRefresh = {
            homeChannelViewModel.items.isNotEmpty()
        }

        triggerRefresh = {
            homeChannelViewModel.refresh()
        }

        homeChannelViewModel.events.filterIsInstance<ChannelUiEvent.RefreshFinished>()
            .onEach { event ->
                refreshState = if (event.success) {
                    RefreshState.RefreshSucceed
                } else {
                    RefreshState.RefreshFailed
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun BottomLoadMoreAdapter.setup(isRefreshing: () -> Boolean) {

        preloadSize = 6

        allowTriggerLoadMore = {
            !isRefreshing() && homeChannelViewModel.items.isNotEmpty()
        }

        canShow = {
            homeChannelViewModel.items.isNotEmpty()
        }

        onTriggerLoadMore = {
            homeChannelViewModel.loadMore()
        }

        homeChannelViewModel.events.filterIsInstance<ChannelUiEvent.LoadMoreFinished>()
            .onEach { event ->
                loadState = if (event.success) {
                    if (event.isFinal) {
                        LoadState.Final
                    } else {
                        LoadState.None
                    }
                } else {
                    LoadState.Error(event.error)
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        homeChannelViewModel.uiState.onEach { state ->
            loadState = if (state.arriveFinalPage) {
                LoadState.Final
            } else {
                LoadState.None
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

    }


    override fun onDestroyView() {
        _binding?.list?.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"

        @JvmStatic
        fun newInstance(index: Int): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_INDEX, index)
                }
            }
        }
    }

}