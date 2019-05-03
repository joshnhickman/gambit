package industries.goodteam.gambit

import android.app.Activity
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import industries.goodteam.gambit.action.Action
import kotlinx.coroutines.*
import kotlin.math.absoluteValue

class Card(
    val ctx: Activity,
    val action: Action,
    val guideline: Guideline,
    val button: Button
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

    init {
        EventBus.register(
            StartCombat::class.java,
            StartRound::class.java,
            FinishRound::class.java,
            ActionPerformed::class.java
        ) {
            if (it is ActionPerformed && it.action == action) animate()
            else if (it !is ActionPerformed) animate()
        }
    }

    private var animation: Job? = null

    private fun animate() {
        ctx.runOnUiThread { button.isEnabled = action.ready() }
        var percent: Float = (guideline.layoutParams as ConstraintLayout.LayoutParams).guidePercent
        var targetPercent = if (action.ready()) top else minOf(top + step * (action.left + 1), bottom)

        GlobalScope.launch { animation?.join() }
        animation = GlobalScope.launch {
            withTimeout(250) {
                while (percent != targetPercent) {
                    if (percent < targetPercent) percent += frame else if (percent > targetPercent) percent -= frame
                    if ((targetPercent - percent).absoluteValue < frame) percent = targetPercent
                    ctx.runOnUiThread { guideline.setGuidelinePercent(percent) }
                    delay(delayMillis)
                }
            }
        }
    }

}