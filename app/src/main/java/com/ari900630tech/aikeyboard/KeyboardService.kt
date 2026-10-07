package com.ari900630tech.aikeyboard

import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.setPadding
import java.util.Locale

class KeyboardService : InputMethodService() {
    private var hebrew = true
    private var shift = false
    private var numbers = false
    private var showingClipboard = false
    private lateinit var box: LinearLayout
    private val prefs by lazy { getSharedPreferences("keyboard_data", Context.MODE_PRIVATE) }
    private val clipPrefs by lazy { getSharedPreferences("clipboard_history", Context.MODE_PRIVATE) }
    private lateinit var clipboard: android.content.ClipboardManager
    private val clipboardListener = android.content.ClipboardManager.OnPrimaryClipChangedListener { captureClipboard() }

    private val he = listOf("קראטוןםפ","שדגכעיחלךף","זסבהנמצתץ")
    private val en = listOf("qwertyuiop","asdfghjkl","zxcvbnm")
    private val commonHe = listOf("שלום","תודה","בוקר","טוב","היום","מחר","אני","אתה","אנחנו","רוצה","צריך","אפשר","בסדר","כן","לא","איפה","מה","איך")
    private val commonEn = listOf("hello","thanks","good","today","tomorrow","I","you","we","want","need","can","yes","no","where","what","how")

    override fun onCreate() {
        super.onCreate()
        clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        if (::clipboard.isInitialized) clipboard.removePrimaryClipChangedListener(clipboardListener)
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4,4,4,4)
            setBackgroundColor(Color.rgb(235,235,240))
        }
        drawKeyboard()
        return box
    }

    private fun drawKeyboard() {
        showingClipboard = false
        box.removeAllViews()
        val suggestions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        suggestionsList().forEach { word -> suggestions.addView(btn(word,1f){ replaceCurrentWord(word) }) }
        box.addView(suggestions,row())

        val rows = if(numbers) listOf("1234567890","@#₪%&*()-_+",".,!?/:;")
        else if(hebrew) he else en
        rows.forEach { chars ->
            val r=LinearLayout(this)
            chars.forEach { c ->
                val shown=if(!hebrew&&!numbers&&shift)c.uppercase(Locale.US) else c.toString()
                r.addView(btn(shown,1f){commit(shown)})
            }
            box.addView(r,row())
        }

        val bottom=LinearLayout(this)
        bottom.addView(btn("⇧",1f){shift=!shift;drawKeyboard()})
        bottom.addView(btn(if(hebrew)"EN" else "עב",1f){hebrew=!hebrew;numbers=false;shift=false;drawKeyboard()})
        bottom.addView(btn(if(numbers)"ABC" else "123",1f){numbers=!numbers;drawKeyboard()})
        bottom.addView(btn("📋",1f){drawClipboard()})
        bottom.addView(btn("😊",1f){commit("😊")})
        bottom.addView(btn("⌫",1f){deleteOne()})
        bottom.addView(btn("↵",1f){enter()})
        box.addView(bottom,row())
    }

    private fun drawClipboard() {
        showingClipboard=true
        box.removeAllViews()
        val title=TextView(this).apply{text="📋  לוח ההעתקות";textSize=19f;gravity=Gravity.CENTER;setPadding(8,8,8,8)}
        box.addView(title,row())
        val controls=LinearLayout(this).apply{gravity=Gravity.CENTER}
        controls.addView(btn("חזרה",1f){drawKeyboard()})
        controls.addView(btn("נקה לא נעוצים",1f){clearUnpinned();drawClipboard()})
        box.addView(controls,row())
        val items=clipboardItems()
        if(items.isEmpty()){
            box.addView(TextView(this).apply{text="אין עדיין טקסטים שמורים";gravity=Gravity.CENTER;textSize=16f},row())
            return
        }
        items.forEach { item ->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            row.addView(btn("📌",0.18f){togglePin(item)})
            row.addView(btn(item,0.64f){commit(item);drawKeyboard()})
            row.addView(btn("✕",0.18f){removeClip(item);drawClipboard()})
            box.addView(row,row())
        }
    }

    private fun captureClipboard() {
        if(!::clipboard.isInitialized || !clipboard.hasPrimaryClip()) return
        val text=clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if(text.isNotEmpty()) saveClip(text)
    }

    private fun clipboardItems():List<String> {
        val all=(0 until clipPrefs.getInt("clip_count",0)).mapNotNull{clipPrefs.getString("clip_$it",null)}
        val pinned=clipPrefs.getStringSet("pinned",emptySet()) ?: emptySet()
        return (all.filter{pinned.contains(it)} + all.filterNot{pinned.contains(it)}).distinct().take(50)
    }

    private fun saveClip(text:String) {
        val old=clipboardItems().toMutableList()
        old.remove(text);old.add(0,text)
        val pinned=clipPrefs.getStringSet("pinned",emptySet()) ?: emptySet()
        clipPrefs.edit().apply {
            old.take(50).forEachIndexed{index,value->putString("clip_$index",value)}
            putInt("clip_count",old.take(50).size)
            putStringSet("pinned",pinned)
            apply()
        }
    }

    private fun removeClip(text:String) {
        val list=clipboardItems().filter{it!=text}
        val pinned=(clipPrefs.getStringSet("pinned",emptySet()) ?: emptySet()).filter{it!=text}.toSet()
        clipPrefs.edit().clear().apply()
        list.forEachIndexed{i,v->clipPrefs.edit().putString("clip_$i",v).apply()}
        clipPrefs.edit().putInt("clip_count",list.size).putStringSet("pinned",pinned).apply()
    }

    private fun togglePin(text:String) {
        val p=(clipPrefs.getStringSet("pinned",emptySet()) ?: emptySet()).toMutableSet()
        if(!p.add(text))p.remove(text)
        clipPrefs.edit().putStringSet("pinned",p).apply()
    }

    private fun clearUnpinned() {
        val p=clipPrefs.getStringSet("pinned",emptySet()) ?: emptySet()
        val keep=clipboardItems().filter{p.contains(it)}
        clipPrefs.edit().clear().apply()
        keep.forEachIndexed{i,v->clipPrefs.edit().putString("clip_$i",v).apply()}
        clipPrefs.edit().putInt("clip_count",keep.size).putStringSet("pinned",p).apply()
    }

    private fun suggestionsList():List<String>{
        val prefix=currentWord()
        val learned=prefs.all.entries.mapNotNull{e->if(e.key.startsWith("word_")) (e.key.removePrefix("word_") to (e.value as? Int ?: 0)) else null}
            .filter{it.first.startsWith(prefix,true)&&it.first.length>prefix.length}
            .sortedByDescending{it.second}.map{it.first}
        if(prefix.isEmpty())return learned.take(3).ifEmpty{if(hebrew)commonHe.take(3) else commonEn.take(3)}
        val common=if(hebrew)commonHe else commonEn
        return(learned+common.filter{it.startsWith(prefix,true)}).distinct().take(3)
    }

    private fun currentWord():String{
        val before=ic()?.getTextBeforeCursor(40,0)?.toString()?:return ""
        return before.split(Regex("""[\s\n\t.,!?;:()\[\]{}"\'/-]+""")).lastOrNull().orEmpty()
    }

    private fun replaceCurrentWord(word:String){
        val prefix=currentWord()
        if(prefix.isNotEmpty())ic()?.deleteSurroundingText(prefix.length,0)
        commit(word);learn(word);commit(" ");drawKeyboard()
    }

    private fun commit(s:String){
        ic()?.commitText(s,1)
        if(s.isNotEmpty() && (s.length>1 || s[0].isLetter()) && s.contains(Regex("[\\s\\n\\t]"))) learnCurrentWord()
        drawSuggestionsOnly()
    }

    private fun drawSuggestionsOnly(){
        if(showingClipboard)return
        if(box.childCount>0){
            val s=box.getChildAt(0) as? LinearLayout ?: return
            s.removeAllViews()
            suggestionsList().forEach{w->s.addView(btn(w,1f){replaceCurrentWord(w)})}
        }
    }

    private fun learnCurrentWord(){
        val before=ic()?.getTextBeforeCursor(80,0)?.toString()?:return
        val word=before.trim().split(Regex("""[\s\n\t.,!?;:()\[\]{}"'/-]+""")).lastOrNull().orEmpty()
        if(word.length>=2)learn(word)
    }

    private fun learn(word:String){
        if(word.length !in 2..32)return
        prefs.edit().putInt("word_$word",prefs.getInt("word_$word",0)+1).apply()
    }

    private fun deleteOne(){ic()?.deleteSurroundingText(1,0);drawSuggestionsOnly()}

    private fun enter(){
        learnCurrentWord()
        val c=ic()?:return
        val action=currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)?:0
        if(action!=0&&action!=EditorInfo.IME_ACTION_UNSPECIFIED)c.performEditorAction(action) else c.commitText("\n",1)
        drawKeyboard()
    }

    private fun row()=LinearLayout.LayoutParams(-1,0,1f).apply{setMargins(1,1,1,1)}

    private fun btn(t:String,w:Float,action:()->Unit)=Button(this).apply{
        text=t;textSize=if(t.length>10)11f else 15f;minWidth=0;minimumWidth=0;setPadding(2,0,2,0)
        background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=12f}
        setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(0,-1,w)
    }

    private fun ic()=currentInputConnection
}
