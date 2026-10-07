package com.ari900630tech.aikeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.util.Locale

class KeyboardService : InputMethodService() {
    private var hebrew = true
    private var shift = false
    private var numbers = false
    private lateinit var box: LinearLayout
    private val prefs by lazy { getSharedPreferences("learned_words", Context.MODE_PRIVATE) }
    private val he = listOf("קראטוןםפ","שדגכעיחלךף","זסבהנמצתץ")
    private val en = listOf("qwertyuiop","asdfghjkl","zxcvbnm")
    private val commonHe = listOf("שלום","תודה","בוקר","טוב","היום","מחר","אני","אתה","אנחנו","רוצה","צריך","אפשר","בסדר","כן","לא","איפה","מה","איך")
    private val commonEn = listOf("hello","thanks","good","today","tomorrow","I","you","we","want","need","can","yes","no","where","what","how")

    override fun onCreateInputView(): View {
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)
            setBackgroundColor(Color.rgb(235, 235, 240))
        }
        draw()
        return box
    }

    private fun draw() {
        box.removeAllViews()
        val suggestions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        suggestionsList().forEach { word ->
            suggestions.addView(btn(word, 1f) { replaceCurrentWord(word) })
        }
        box.addView(suggestions, row())

        val rows = if (numbers) listOf("1234567890", "@#₪%&*()-_+", ".,!?/:;")
        else if (hebrew) he else en

        rows.forEach { chars ->
            val r = LinearLayout(this)
            chars.forEach { c ->
                val shown = if (!hebrew && !numbers && shift) c.uppercase(Locale.US) else c.toString()
                r.addView(btn(shown, 1f) { commit(shown) })
            }
            box.addView(r, row())
        }

        val bottom = LinearLayout(this)
        bottom.addView(btn("⇧", 1f) { shift = !shift; draw() })
        bottom.addView(btn(if (hebrew) "EN" else "עב", 1f) { hebrew = !hebrew; numbers = false; shift = false; draw() })
        bottom.addView(btn(if (numbers) "ABC" else "123", 1f) { numbers = !numbers; draw() })
        bottom.addView(btn("😊", 1f) { commit("😊") })
        bottom.addView(btn("⌫", 1f) { deleteOne() })
        bottom.addView(btn("↵", 1f) { enter() })
        box.addView(bottom, row())
    }

    private fun suggestionsList(): List<String> {
        val prefix = currentWord()
        val learned = prefs.all.entries
            .mapNotNull { e -> (e.value as? Int)?.let { e.key to it } }
            .filter { it.first.startsWith(prefix, ignoreCase = true) && it.first.length > prefix.length }
            .sortedByDescending { it.second }
            .map { it.first }
        if (prefix.isEmpty()) {
            return learned.take(3).ifEmpty { if (hebrew) commonHe.take(3) else commonEn.take(3) }
        }
        val common = if (hebrew) commonHe else commonEn
        return (learned + common.filter { it.startsWith(prefix, ignoreCase = true) })
            .distinct()
            .take(3)
    }

    private fun currentWord(): String {
        val before = ic()?.getTextBeforeCursor(40, 0)?.toString() ?: return ""
        return before.substringAfterLast(Regex("[\\s\\n\\t.,!?;:()\\[\\]{}"'/-]"))
    }

    private fun replaceCurrentWord(word: String) {
        val prefix = currentWord()
        if (prefix.isNotEmpty()) ic()?.deleteSurroundingText(prefix.length, 0)
        commit(word)
        learn(word)
        commit(" ")
        draw()
    }

    private fun commit(s: String) {
        ic()?.commitText(s, 1)
        if (s.length == 1 && !s[0].isLetter()) return
        if (s.contains(Regex("[\\s\\n\\t]"))) learnCurrentWord()
        draw()
    }

    private fun learnCurrentWord() {
        val before = ic()?.getTextBeforeCursor(80, 0)?.toString() ?: return
        val word = before.trim().split(Regex("[\\s\\n\\t.,!?;:()\\[\\]{}"'/-]+")).lastOrNull().orEmpty()
        if (word.length >= 2) learn(word)
    }

    private fun learn(word: String) {
        if (word.length < 2 || word.length > 32) return
        val old = prefs.getInt(word, 0)
        prefs.edit().putInt(word, (old + 1).coerceAtMost(100000)).apply()
    }

    private fun deleteOne() {
        ic()?.deleteSurroundingText(1, 0)
        draw()
    }

    private fun enter() {
        learnCurrentWord()
        val c = ic() ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: 0
        if (action != 0 && action != EditorInfo.IME_ACTION_UNSPECIFIED) c.performEditorAction(action)
        else c.commitText("\n", 1)
        draw()
    }

    private fun row() = LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(1, 1, 1, 1) }

    private fun btn(t: String, w: Float, action: () -> Unit) = Button(this).apply {
        text = t
        textSize = if (t.length > 1) 14f else 19f
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 12f }
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(0, -1, w)
    }

    private fun ic() = currentInputConnection
}
