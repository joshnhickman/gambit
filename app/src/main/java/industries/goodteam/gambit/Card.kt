package industries.goodteam.gambit

import android.app.Activity
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import industries.goodteam.gambit.action.Action
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class Card(
    val context: Activity,
    val action: Action,
    val guideline: Guideline,
    val button: Button,
    val text: TextView
) {

    companion object {
        const val delayMillis = 16L
        const val top = 0.7f
        const val bottom = 0.9f
        const val range = bottom - top
        const val maxCooldown = 5
        const val step = range / maxCooldown
        const val frame = 0.01f
    }

    var animation: Job? = null

    fun animate() {
        context.runOnUiThread { button.isEnabled = action.ready() }
        var percent: Float = (guideline.layoutParams as ConstraintLayout.LayoutParams).guidePercent
        var targetPercent = if (action.ready()) top else minOf(top + step * (action.left + 1), bottom)
        animation?.cancel()
        animation = GlobalScope.launch {
            while (percent != targetPercent) {
                if (percent < targetPercent) percent += frame else if (percent > targetPercent) percent -= frame
                if ((targetPercent - percent).absoluteValue < frame) percent = targetPercent
                context.runOnUiThread { guideline.setGuidelinePercent(percent) }
                delay(delayMillis)
            }
        }
    }

}