package industries.goodteam.gambit

import android.app.Activity
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import industries.goodteam.gambit.action.Action
import industries.goodteam.gambit.event.EventBus
import industries.goodteam.gambit.event.FinishRound
import industries.goodteam.gambit.event.StartRound
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class Card(
    private val ctx: Activity,
    private val action: Action,
    private val guideline: Guideline,
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
        EventBus.registerUI(StartRound::class.java, FinishRound::class.java) { animate() }
    }

    private var animation: Job? = null

    private fun animate() {
        val targetPercent = if (action.ready()) top else minOf(top + step * (action.left + 1), bottom)
        if ((guideline.layoutParams as ConstraintLayout.LayoutParams).guidePercent != targetPercent) {
            animation?.cancel()
            animation = GlobalScope.launch {
                while (true) {
                    var percent = (guideline.layoutParams as ConstraintLayout.LayoutParams).guidePercent
                    if (percent < targetPercent) percent += frame else if (percent > targetPercent) percent -= frame
                    if ((targetPercent - percent).absoluteValue < frame) percent = targetPercent
                    ctx.runOnUiThread { guideline.setGuidelinePercent(percent) }
                    delay(delayMillis)
                }
            }
        }
    }

}