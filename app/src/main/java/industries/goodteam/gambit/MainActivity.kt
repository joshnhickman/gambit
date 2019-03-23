package industries.goodteam.gambit

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.entity.Entity
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class MainActivity : AppCompatActivity() {

    val attack = Attack(0)
    val defend = Defend(1)
    val stun = Stun(2)

    lateinit var player: Entity
    lateinit var enemy: Entity

    lateinit var enemyNameText: TextView
    lateinit var enemyActionText: TextView

    lateinit var defendButton: Button
    lateinit var attackButton: Button
    lateinit var stunButton: Button

    lateinit var attackText: TextView
    lateinit var defendText: TextView
    lateinit var stunText: TextView

    lateinit var playerHealthBar: ProgressBar
    lateinit var enemyHealthBar: ProgressBar

    lateinit var playerHealthText: TextView
    lateinit var enemyHealthText: TextView

    lateinit var eventsText: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set fullscreen options
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        setContentView(R.layout.combat)

        init()

        find<Button>(R.id.editButton).onClick {
            alert("Edit Stats") {
                customView {
                    verticalLayout {
                        var editStats = { stat: String, value: Int, desc: String, function: (Int) -> Unit ->
                            linearLayout {
                                textView(stat)
                                editText {
                                    inputType = InputType.TYPE_CLASS_NUMBER
                                    setText("${value}")
                                    afterTextChanged {
                                        function(it.getIntWithBlank())
                                        draw()
                                    }
                                }
                                textView(desc)
                            }
                        }
                        padding = dip(10)

                        editStats("pvitality", player.vitality, "max health") { player.vitality = it }
                        editStats("phealth", player.health, "current health") { player.health = it }
                        editStats("pluck", player.luck, "not used") { player.luck = it }
                        editStats("pstrength", player.strength, "max damage") { player.strength = it }
                        editStats("paccuracy", player.accuracy, "min damage (<str)") { player.accuracy = it }
                        editStats("parmor", player.armor, "max shield") { player.armor = it }
                        editStats("preflexes", player.reflexes, "min shield (<armor)") { player.reflexes = it }
                        editStats("pconcentration", player.concentration, "stun duration") { player.concentration = it }

                        editStats("evitality", enemy.vitality, "") { enemy.vitality = it }
                        editStats("ehealth", enemy.health, "") { enemy.health = it }
                        editStats("eluck", enemy.luck, "") { enemy.luck = it }
                        editStats("estrength", enemy.strength, "") { enemy.strength = it }
                        editStats("eaccuracy", enemy.accuracy, "") { enemy.accuracy = it }
                        editStats("earmor", enemy.armor, "") { enemy.armor = it }
                        editStats("ereflexes", enemy.reflexes, "") { enemy.reflexes = it }
                        editStats("econcentration", enemy.concentration, "") { enemy.concentration = it }
                    }
                }
                yesButton { toast("saved") }
            }.show()
        }

        defendButton = find<Button>(R.id.defendButton).apply { onClick { act(defend) } }
        defendText = find<TextView>(R.id.defendText)

        attackButton = find<Button>(R.id.attackButton).apply { onClick { act(attack) } }
        attackText = find<TextView>(R.id.attackText)

        stunButton = find<Button>(R.id.utilityButton).apply { onClick { act(stun) } }
        stunText = find<TextView>(R.id.stunText)

        playerHealthBar = find<ProgressBar>(R.id.healthBar)
        enemyHealthBar = find<ProgressBar>(R.id.enemyHealthBar)

        playerHealthText = find<TextView>(R.id.playerHealthText)
        enemyHealthText = find<TextView>(R.id.enemyHealthText)

        enemyNameText = find<TextView>(R.id.enemyNameText)
        enemyActionText = find<TextView>(R.id.enemyActionText)

        eventsText = find<TextView>(R.id.eventsText).apply { text = "last turn:" }

        update()
    }

    private fun init() {
        player = Entity(
            name = "player",
            luck = 1,
            vitality = 40,
            strength = 4,
            accuracy = 1,
            armor = 4,
            reflexes = 1,
            concentration = 2,
            actions = *arrayOf(attack, defend, stun)
        )
        enemy = Entity(
            name = "enemy",
            luck = 1,
            vitality = 30,
            strength = 5,
            accuracy = 1,
            armor = 5,
            reflexes = 1,
            actions = *arrayOf(Attack(0), Defend(1))
        )
    }

    private fun act(action: Action) {
        var events = mutableListOf<String>()
        player.intend(action)

        if (action is Stun) {
            var duration = enemy.stun(player.concentration)
            events.add("you stunned ${enemy.name} for ${duration} turns")
        }
        if (enemy.intent is Stun) {
            var duration = player.intend(Wait())
            events.add("${enemy.name} stunned you for ${duration} turns")
        }

        if (action is Defend) {
            var amt = player.defend()
            events.add("you prepare to defend ${amt} damage")
        }
        if (enemy.intent is Defend) {
            events.add("${enemy.name} prepares to defend ${enemy.defend()} damage")
        }

        if (action is Attack) {
            var damage = player.actionValue().random()
            var actualDamage = enemy.hit(damage)
            events.add("you attack for ${damage} damage")
            events.add("enemy takes ${actualDamage} damage")
        }
        if (enemy.intent is Attack) {
            var damage = enemy.actionValue().random()
            var actualDamage = player.hit(damage)
            events.add("${enemy.name} attacks for ${damage} damage")
            events.add("you take ${actualDamage} damage")
        }

        if (action is Wait) {
            events.add("you do nothing")
        }
        if (enemy.intent is Wait) {
            events.add("${enemy.name} does nothing")
        }

        eventsText.setText("last turn:\n${events.joinToString("\n")}")

        player.act()
        enemy.act()

        if (!enemy.alive()) {
            longToast("WINNER")
            init()
        } else if (!player.alive()) {
            longToast("LOSER")
            init()
        }

        update()
    }

    private fun update() {
        player.update()
        enemy.update()

        enemy.intend()

        draw()
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

        defendText.text = "block ${player.actionValue(defend)} damage"
        attackText.text = "deal ${player.actionValue(attack)} damage"
        stunText.text = "stun for ${player.actionValue(stun)} turn(s)"

        playerHealthText.text = "${player.health} / ${player.vitality}"
        enemyHealthText.text = "${enemy.health} / ${enemy.vitality}"

        if (player.stunned()) {
//            TODO: how to advance turn?
            defendButton.visibility = View.GONE
            attackButton.visibility = View.GONE
            stunButton.visibility = View.GONE
        } else {
            defendButton.visibility = if (defend.ready()) View.VISIBLE else View.GONE
            attackButton.visibility = if (attack.ready()) View.VISIBLE else View.GONE
            stunButton.visibility = if (stun.ready()) View.VISIBLE else View.GONE
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
        try {
            return this.toInt()
        } catch (e: NumberFormatException) {
            return 0
        }
    }
}
