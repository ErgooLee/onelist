package io.github.ergoo.onelist

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class OneItemAdapterTest {

    enum class State { LOADING, ERROR, EMPTY, NONE }

    private class TestOneItemAdapter(initState: State = State.NONE) :
        OneItemAdapter<State, RecyclerView.ViewHolder>(initState) {

        var lastBoundStatus: State? = null

        override fun isValid(status: State): Boolean = status != State.NONE

        override fun onCreateViewHolderByStatus(
            parent: ViewGroup, status: State
        ): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
        }

        override fun onBindViewHolderByStatus(holder: RecyclerView.ViewHolder, status: State) {
            lastBoundStatus = status
        }
    }

    @Test
    fun `initial NONE state has itemCount 0`() {
        val adapter = TestOneItemAdapter(State.NONE)
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `initial LOADING state has itemCount 1`() {
        val adapter = TestOneItemAdapter(State.LOADING)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `changing from NONE to LOADING increases itemCount`() {
        val adapter = TestOneItemAdapter(State.NONE)
        adapter.status = State.LOADING
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `changing from LOADING to NONE decreases itemCount`() {
        val adapter = TestOneItemAdapter(State.LOADING)
        adapter.status = State.NONE
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `changing between valid states keeps itemCount at 1`() {
        val adapter = TestOneItemAdapter(State.LOADING)
        adapter.status = State.ERROR
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `setting same status does not crash`() {
        val adapter = TestOneItemAdapter(State.LOADING)
        adapter.status = State.LOADING
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `getItem returns current status`() {
        val adapter = TestOneItemAdapter(State.ERROR)
        assertEquals(State.ERROR, adapter.getItem(0))
        adapter.status = State.EMPTY
        assertEquals(State.EMPTY, adapter.getItem(0))
    }

    @Test
    fun `full lifecycle NONE - LOADING - ERROR - NONE`() {
        val adapter = TestOneItemAdapter(State.NONE)
        assertEquals(0, adapter.itemCount)
        adapter.status = State.LOADING
        assertEquals(1, adapter.itemCount)
        adapter.status = State.ERROR
        assertEquals(1, adapter.itemCount)
        adapter.status = State.NONE
        assertEquals(0, adapter.itemCount)
    }
}

