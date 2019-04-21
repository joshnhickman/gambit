package industries.goodteam.gambit

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.os.Bundle
import android.support.constraint.Guideline
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.effect.Effect
import industries.goodteam.gambit.entity.Entity
import industries.goodteam.gambit.entity.Player
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onTouch

class MainActivity : AppCompatActivity() {

    private val log = AnkoLogger(this.javaClass)

    private val attack = Attack(0)
    private val defend = Defend(1)
    private val stun = Stun(3)
    private var steal = Steal(0)

    private lateinit var player: Player
    private lateinit var enemy: Entity

    private lateinit var enemies: List<Entity>

    private lateinit var enemyNameText: TextView
    private lateinit var enemyActionText: TextView

    private lateinit var defendButton: Button
    private lateinit var attackButton: Button
    private lateinit var stunButton: Button
    private lateinit var stealButton: Button
    private lateinit var waitButton: Button

    private lateinit var attackText: TextView
    private lateinit var defendText: TextView
    private lateinit var stunText: TextView
    private lateinit var stealText: TextView

    private lateinit var playerHealthBar: ProgressBar
    private lateinit var enemyHealthBar: ProgressBar

    private lateinit var playerDamageText: TextView
    private lateinit var enemyDamageText: TextView

    private lateinit var playerHealthText: TextView
    private lateinit var enemyHealthText: TextView

    private lateinit var eventsText: TextView
    private lateinit var goldText: TextView

    private lateinit var guideline: Guideline

    private var events = mutableListOf<String>()
    private var level = 0
    private var combat = -1
    private var round = 0

    private var defeated = mutableListOf<Entity>()

    // TODO: implement performClick for onTouchListeners and disable suppression
    @SuppressLint("ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set fullscreen options
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        setContentView(R.layout.combat)

        // get handles to ui
        defendButton = find<Button>(R.id.defendButton).apply { onClick { act(defend) } }
        defendText = find(R.id.defendText)

        attackButton = find<Button>(R.id.attackButton).apply { onClick { act(attack) } }
        attackText = find(R.id.attackText)

        stunButton = find<Button>(R.id.utilityButton).apply { onClick { act(stun) } }
        stunText = find(R.id.stunText)

        stealButton = find<Button>(R.id.stealButton).apply { onClick { act(steal) } }
        stealText = find(R.id.stealText)

        waitButton = find<Button>(R.id.waitButton).apply { onClick { act(Wait()) } }

        playerHealthBar = find(R.id.healthBar)
        enemyHealthBar = find(R.id.enemyHealthBar)

        playerHealthText = find(R.id.playerHealthText)
        enemyHealthText = find(R.id.enemyHealthText)

        playerDamageText = find<TextView>(R.id.playerDamage).apply { alpha = 0f }
        enemyDamageText = find<TextView>(R.id.enemyDamage).apply { alpha = 0f }

        enemyNameText = find(R.id.enemyNameText)
        enemyActionText = find(R.id.enemyActionText)

        eventsText = find(R.id.eventsText)
        goldText = find(R.id.goldText)

        guideline = find(R.id.attackGuideline)

        // get full y of display
        var size = Point()
        windowManager.defaultDisplay.getSize(size)
        val windowY = size.y

        var lastY = 0f
        var lastPercent = 0.7f
        find<LinearLayout>(R.id.attackCard).setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_MOVE -> {
                    lastPercent -= (lastY - event.rawY) / windowY
                    guideline.setGuidelinePercent(lastPercent)
                }
                MotionEvent.ACTION_UP -> {
                    if (lastPercent < 0.5f) {
                        act(attack)
                    }
                    GlobalScope.launch {
                        while(lastPercent < 0.7f) {
                            lastPercent += 0.05f
                            if (lastPercent > 0.7f) lastPercent = 0.7f
                            runOnUiThread { guideline.setGuidelinePercent(lastPercent) }
                            delay(16)
                        }
                    }
                }
            }
            lastY = event.rawY
            true
        }

        // set up buttons
        find<Button>(R.id.restartButton).onClick {
            log.info("player requested new game")
            newGame()
        }

        find<Button>(R.id.editButton).onClick {
            alert("Edit Stats") {
                customView {
                    verticalLayout {
                        var editStats = { stat: String, value: Int, desc: String, function: (Int) -> Unit ->
                            linearLayout {
                                textView(stat)
                                editText {
                                    padding = dip(3)
                                    textSize = 14f
                                    inputType = InputType.TYPE_CLASS_NUMBER
                                    setText("$value")
                                    afterTextChanged {
                                        function(it.getIntWithBlank())
                                        draw()
                                    }
                                }
                                textView(desc)
                            }
                        }
                        padding = dip(10)

                        editStats("pVitality", player.vitality, "max health") { player.vitality = it }
                        editStats("pHealth", player.health, "current health") { player.health = it }
                        editStats("pLuck", player.luck, "not used") { player.luck = it }
                        editStats("pStrength", player.strength, "max damage") { player.strength = it }
                        editStats("pAccuracy", player.accuracy, "min damage (<str)") { player.accuracy = it }
                        editStats("pArmor", player.armor, "max shield") { player.armor = it }
                        editStats("pReflexes", player.reflexes, "min shield (<armor)") { player.reflexes = it }
                        editStats("pConcentration", player.concentration, "stun duration") { player.concentration = it }

                        editStats("pDefendCooldown", defend.cooldown, "") { defend.cooldown = it }
                        editStats("pAttackCooldown", attack.cooldown, "") { attack.cooldown = it }
                        editStats("pStunCooldown", stun.cooldown, "") { stun.cooldown = it }

                        editStats("eVitality", enemy.vitality, "") { enemy.vitality = it }
                        editStats("eHealth", enemy.health, "") { enemy.health = it }
                        editStats("eLuck", enemy.luck, "not used") { enemy.luck = it }
                        editStats("eStrength", enemy.strength, "") { enemy.strength = it }
                        editStats("eAccuracy", enemy.accuracy, "") { enemy.accuracy = it }
                        editStats("eArmor", enemy.armor, "") { enemy.armor = it }
                        editStats("eReflexes", enemy.reflexes, "") { enemy.reflexes = it }
                        editStats("eConcentration", enemy.concentration, "") { enemy.concentration = it }
                    }
                }
                yesButton { toast("saved") }
            }.show()
        }

        newGame()
    }

    private fun newGame() {
        log.info("start new game")
        level = 0
        events.clear()
        defeated.clear()
        player = Player(
            luck = 1,
            vitality = 40,
            strength = 4,
            accuracy = 1,
            armor = 4,
            reflexes = 1,
            concentration = 2,
            attack = attack,
            defend = defend,
            stun = stun,
            steal = steal
        )

        newLevel()
    }

    private fun newLevel() {
        if (level > 0) {
            val cost = level * 50
            alert("heal ${player.vitality / 2} health for $cost gold?") {
                if (player.gold >= cost) {
                    yesButton {
                        player.gold -= level * 50
                        player.heal(player.vitality / 2)
                        draw()
                    }
                }
                noButton {}
            }.show()
        }
        combat = 0
        enemies = mutableListOf(
            Entity(
                name = "generic",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = *arrayOf(Attack(0), Defend(1))
            ),
            Entity(
                name = "defender",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 20 + level,
                reflexes = 5 + level,
                actions = *arrayOf(Attack(1, 1), Defend(0))),
            Entity(
                name = "stunner",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = *arrayOf(Attack(0), Stun(4, 2))),
            Entity(
                name = "damager",
                luck = 1 + level,
                vitality = 30 + 5 * level,
                strength = 20 + level,
                accuracy = 15 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                actions = *arrayOf(Attack(4, 4), Defend(0))),
            Entity(
                name = "weakener",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = *arrayOf(Attack(0), Defend(1), Modify(Effect(StatType.STRENGTH, -2 - level, 2), 2))
            )
        ).shuffled()

        newCombat()
    }

    private fun newCombat() {
        log.info("start new combat")
        player.endCombat()

        combat++

        if (combat == enemies.size + 1) {
            longToast("level $level complete, advancing")
            level++
            newLevel()
        } else {
            enemy = enemies[combat - 1]
            events.add("encountered enemy ${enemy.name}")
            events.add("++ start combat $combat ++")

            round = 0
            newRound()
        }
    }

    private fun newRound() {
        log.info("start new round")
        round++
        events.add("-- start round $round --")

        enemy.intend()
        events.add("${enemy.name} intends to ${enemy.intent.name} ${enemy.actionValue()}")

        draw()
    }

    private fun act(action: Action) {
        log.info("act $action")
        player.intend(action)

        if (player.intent is Stun) {
            val duration = enemy.stun(player.concentration)
            events.add("you stunned ${enemy.name} for $duration turns")
        }
        if (enemy.intent is Stun) {
            val duration = player.stun(enemy.concentration )
            events.add("${enemy.name} stunned you for $duration turns")
        }

        if (player.intent is Modify) {}
        if (enemy.intent is Modify) {
            val effect = (enemy.intent as Modify).effect.apply()
            player.affect(effect)
            events.add("${enemy.name} modified your ${effect.targetStat} by ${effect.value}")
        }

        if (player.intent is Defend) {
            val amt = player.defend()
            events.add("you prepare to defend $amt damage")
        }
        if (enemy.intent is Defend) events.add("${enemy.name} prepares to defend ${enemy.defend()} damage")

        if (player.alive() && player.intent is Attack) {
            val damage = player.actionValue().random()
            val actualDamage = enemy.damage(damage)
            events.add("you attack for $damage damage")
            events.add("${enemy.name} takes $actualDamage damage")
            GlobalScope.launch {
                enemyDamageText.text = "$actualDamage"
                enemyDamageText.alpha = 1.0f
                while(enemyDamageText.alpha > 0) {
                    runOnUiThread { enemyDamageText.alpha -= 0.01f }
                    delay(16)
                }
            }
        }
        if (enemy.alive() && enemy.intent is Attack) {
            val damage = enemy.actionValue().random()
            val actualDamage = player.damage(damage)
            events.add("${enemy.name} attacks for $damage damage")
            events.add("you take $actualDamage damage")
            GlobalScope.launch {
                playerDamageText.text = "$actualDamage"
                playerDamageText.alpha = 1.0f
                while(playerDamageText.alpha > 0) {
                    runOnUiThread { playerDamageText.alpha -= 0.01f }
                    delay(16)
                }
            }
        }

        if (player.intent is Steal) {
            val stolen = player.actionValue().random()
            player.gold += stolen
            events.add("you steal $stolen gold")
        }

        if (player.intent is Wait) events.add("you do nothing")
        if (enemy.intent is Wait) events.add("${enemy.name} does nothing")

        player.act()
        enemy.act()

        if (!enemy.alive()) {
            events.add("${enemy.name} dies")
            events.add("++ end combat $combat ++")
            defeated.add(enemy)
            alert("defeated ${enemy.name}") { yesButton {} }.show()
            newCombat()
        } else if (!player.alive()) {
            alert("""
                ${enemy.name} defeated you
                stole ${player.gold} gold
                defeated ${defeated.size} enemies:
                ${defeated.joinToString(",") { it.name }}
            """.trimIndent()) { yesButton {} }.show()
            newGame()
        } else {
            player.update()
            enemy.update()
            newRound()
        }
    }

    private fun draw() {
        playerHealthBar.apply {
            max = player.vitality
            progress = player.health
        }
        enemyHealthBar.apply {
            max = enemy.vitality
            progress = enemy.health
        }

        enemyNameText.text = enemy.name
        enemyActionText.text = "${enemy.intent.name} ${enemy.actionValue()}"

        defendText.text = """*block ${player.actionValue(defend)} damage
            |*${defend.cooldown} turn cooldown
        """.trimMargin()
        attackText.text = """*deal ${player.actionValue(attack)} damage
            |*damage increases with consecutive attacks
            |*${attack.cooldown} turn cooldown
        """.trimMargin()
        stunText.text = """*stun for ${player.actionValue(stun)} turn(s)
            |*${stun.cooldown} turn cooldown
        """.trimMargin()
        stealText.text = """*steal ${player.actionValue(steal)} gold
            |*${steal.cooldown} turn cooldown
        """.trimMargin()

        playerHealthText.text = "${player.health} / ${player.vitality}"
        enemyHealthText.text = "${enemy.health} / ${enemy.vitality}"

        goldText.text = "GOLD: ${player.gold}"

        eventsText.text = events.takeLast(10).joinToString("\n")

        if (player.stunned()) {
            defendButton.visibility = View.GONE
//            attackButton.visibility = View.GONE
            stunButton.visibility = View.GONE
            stealButton.visibility = View.GONE
            waitButton.visibility = View.VISIBLE
        } else {
            defendButton.visibility = if (defend.ready()) View.VISIBLE else View.GONE
//            attackButton.visibility = if (attack.ready()) View.VISIBLE else View.GONE
            stunButton.visibility = if (stun.ready()) View.VISIBLE else View.GONE
            stealButton.visibility = if (steal.ready()) View.VISIBLE else View.GONE
            waitButton.visibility = View.GONE
        }
    }

    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                afterTextChanged.invoke(editable.toString())
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun String.getIntWithBlank(): Int {
        return try {
            this.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }
}
