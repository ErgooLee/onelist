package io.github.ergoo.onelist

import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class EmptyContentAdapterTest {

    private val context get() = RuntimeEnvironment.getApplication()

    private fun createEmptyView(): View {
        return FrameLayout(context).apply {
            id = View.generateViewId()
        }
    }

    @Test
    fun `initial status is true so itemCount is 1`() {
        val adapter = EmptyContentAdapter(createEmptyView())
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `setting status to false hides the view`() {
        val adapter = EmptyContentAdapter(createEmptyView())
        adapter.status = false
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setting status back to true shows the view`() {
        val adapter = EmptyContentAdapter(createEmptyView())
        adapter.status = false
        assertEquals(0, adapter.itemCount)
        adapter.status = true
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `getStateViewType returns emptyView id`() {
        val emptyView = createEmptyView()
        val adapter = EmptyContentAdapter(emptyView)
        assertEquals(emptyView.id, adapter.getStateViewType(true))
        assertEquals(emptyView.id, adapter.getStateViewType(false))
    }

    @Test
    fun `isValid returns true for true status`() {
        val adapter = EmptyContentAdapter(createEmptyView())
        assertTrue(adapter.isValid(true))
    }

    @Test
    fun `isValid returns false for false status`() {
        val adapter = EmptyContentAdapter(createEmptyView())
        assertFalse(adapter.isValid(false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws if emptyView has no id`() {
        val view = FrameLayout(context) // id is NO_ID by default
        EmptyContentAdapter(view)
    }

    @Test
    fun `implements FullSpan`() {
        val adapter = EmptyContentAdapter(createEmptyView())
        assertTrue(adapter is FullSpan)
    }

    @Test
    fun `bindToContentAdapter updates status when content changes`() {
        val emptyView = createEmptyView()
        val emptyAdapter = EmptyContentAdapter(emptyView)

        // Create a simple content adapter with mutable item count
        val contentAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            var count = 0
                set(value) {
                    field = value
                    notifyDataSetChanged()
                }

            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = count
        }

        // Manually bind (bypasses ConcatAdapter auto-discovery)
        emptyAdapter.bindToContentAdapter(contentAdapter)

        // Content is empty → empty view visible
        assertTrue(emptyAdapter.status)
        assertEquals(1, emptyAdapter.itemCount)

        // Add content → empty view hidden
        contentAdapter.count = 5
        assertFalse(emptyAdapter.status)
        assertEquals(0, emptyAdapter.itemCount)

        // Clear content → empty view visible again
        contentAdapter.count = 0
        assertTrue(emptyAdapter.status)
        assertEquals(1, emptyAdapter.itemCount)
    }

    @Test
    fun `unBind stops observing content adapter`() {
        val emptyView = createEmptyView()
        val emptyAdapter = EmptyContentAdapter(emptyView)

        val contentAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            var count = 0
                set(value) {
                    field = value
                    notifyDataSetChanged()
                }

            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount() = count
        }

        emptyAdapter.bindToContentAdapter(contentAdapter)
        assertTrue(emptyAdapter.status)

        contentAdapter.count = 5
        assertFalse(emptyAdapter.status)

        // Unbind
        emptyAdapter.unBind()

        // Changes to content should no longer affect empty adapter
        contentAdapter.count = 0
        assertFalse(emptyAdapter.status) // stays false since unbound
    }
}

