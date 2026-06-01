package io.github.ergoo.onelist.demo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.github.ergoo.onelist.demo.databinding.ViewStateLayoutBinding

/**
 * A multi-state view that supports loading, empty, and error states.
 * The error state includes an optional retry button.
 *
 * Usage example:
 * ```xml
 * <io.github.ergoo.onelist.demo.LoadStateView
 *     android:id="@+id/loadStateView"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 * ```
 *
 * Kotlin:
 * ```kotlin
 * loadStateView.showLoading()
 * loadStateView.showEmpty(message = "No content")
 * loadStateView.showError(message = "Network error") { /* retry */ }
 * loadStateView.hide()
 * ```
 */
class LoadStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    enum class State { GONE, LOADING, EMPTY, ERROR }

    private val binding: ViewStateLayoutBinding =
        ViewStateLayoutBinding.inflate(LayoutInflater.from(context), this)

    private var retryListener: (() -> Unit)? = null

    /**
     * Minimum duration (ms) for the loading state to stay visible.
     * Prevents flickering when the data loads faster than this threshold.
     * Default is 0 (no minimum).
     */
    var minLoadingDurationMs: Long = 0L

    private var loadingStartTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null

    /** Currently displayed state */
    var currentState: State = State.GONE
        private set

    init {
        // Read XML attributes
        val ta = context.obtainStyledAttributes(attrs, R.styleable.LoadStateView)
        try {
            ta.getString(R.styleable.LoadStateView_sl_loadingMessage)?.let {
                binding.tvLoadingMessage.text = it
            }
            ta.getString(R.styleable.LoadStateView_sl_emptyMessage)?.let {
                binding.tvEmptyMessage.text = it
            }
            ta.getString(R.styleable.LoadStateView_sl_emptySubMessage)?.let {
                binding.tvEmptySubMessage.text = it
                // Text only; visibility is controlled by the Group
            }
            ta.getResourceId(R.styleable.LoadStateView_sl_emptyIcon, 0).takeIf { it != 0 }?.let {
                binding.ivEmptyIcon.setImageResource(it)
            }
            ta.getString(R.styleable.LoadStateView_sl_errorMessage)?.let {
                binding.tvErrorMessage.text = it
            }
            ta.getString(R.styleable.LoadStateView_sl_errorSubMessage)?.let {
                binding.tvErrorSubMessage.text = it
                // Text only; visibility is controlled by the Group
            }
            ta.getResourceId(R.styleable.LoadStateView_sl_errorIcon, 0).takeIf { it != 0 }?.let {
                binding.ivErrorIcon.setImageResource(it)
            }
            ta.getString(R.styleable.LoadStateView_sl_retryText)?.let {
                binding.btnRetry.text = it
            }
        } finally {
            ta.recycle()
        }

        binding.btnRetry.setOnClickListener { retryListener?.invoke() }

        // Hide all states initially
        hide()

        if (id == NO_ID) {
            // Assign a default ID if none is set, to ensure state saving works
            id = generateViewId()
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Show the loading state */
    fun showLoading(message: String? = null) {
        cancelPending()
        loadingStartTime = SystemClock.elapsedRealtime()
        message?.let { binding.tvLoadingMessage.text = it }
        switchTo(State.LOADING)
    }

    /** Show the empty state */
    fun showEmpty(
        message: String? = null,
        subMessage: String? = null
    ) {
        cancelPending()
        message?.let { binding.tvEmptyMessage.text = it }
        binding.tvEmptySubMessage.text = subMessage ?: ""
        switchTo(State.EMPTY)
        binding.tvEmptySubMessage.visibility =
            if (subMessage.isNullOrEmpty()) GONE else VISIBLE
    }

    /**
     * Show the error state.
     * @param message     Primary error message.
     * @param subMessage  Secondary error message (optional).
     * @param retryAction Callback for the retry button; pass null to hide the button.
     */
    fun showError(
        message: String? = null,
        subMessage: String? = null,
        retryAction: (() -> Unit)? = null
    ) {
        scheduleOrRun {
            message?.let { binding.tvErrorMessage.text = it }
            binding.tvErrorSubMessage.text = subMessage ?: ""
            retryListener = retryAction
            switchTo(State.ERROR)
            binding.tvErrorSubMessage.visibility =
                if (subMessage.isNullOrEmpty()) GONE else VISIBLE
            binding.btnRetry.visibility =
                if (retryAction != null) VISIBLE else GONE
        }
    }

    /** Hide all states and restore normal content visibility */
    fun hide() {
        cancelPending()
        switchTo(State.GONE)
    }

    /** Set or replace the retry click listener */
    fun setOnRetryClickListener(listener: () -> Unit) {
        retryListener = listener
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Used only for loading → error transitions.
     * If [minLoadingDurationMs] has not elapsed since [showLoading] was called,
     * delays [action] for the remaining time; otherwise runs it immediately.
     */
    private fun scheduleOrRun(action: () -> Unit) {
        cancelPending()
        if (currentState == State.LOADING && minLoadingDurationMs > 0) {
            val elapsed = SystemClock.elapsedRealtime() - loadingStartTime
            val remaining = minLoadingDurationMs - elapsed
            if (remaining > 0) {
                val r = Runnable { action() }
                pendingRunnable = r
                handler.postDelayed(r, remaining)
                return
            }
        }
        action()
    }

    private fun cancelPending() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
    }

    private fun switchTo(state: State) {
        currentState = state
        binding.groupLoading.visibility = if (state == State.LOADING) VISIBLE else GONE
        binding.groupEmpty.visibility = if (state == State.EMPTY) VISIBLE else GONE
        binding.groupError.visibility = if (state == State.ERROR) VISIBLE else GONE
        // Hide LoadStateView itself when idle so it doesn't intercept touch events
        visibility = if (state == State.GONE) GONE else VISIBLE
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelPending()
    }
}


