package industries.goodteam.gambit.effect

import industries.goodteam.gambit.Stat

class AppliedEffect(
    val effect: Effect,
    val targetStat: Stat = effect.targetStat,
    val value: Int = effect.value,
    val duration: Duration = effect.duration,
    var left: Int = duration
) {

    fun update() {
        left--
    }

    fun done(): Boolean = left < 0

}