package com.ari900630tech.aikeyboard
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.util.Locale
class KeyboardService:InputMethodService(){
 private var hebrew=true;private var shift=false;private var numbers=false;private lateinit var box:LinearLayout
 private val he=listOf("קראטוןםפ","שדגכעיחלךף","זסבהנמצתץ")
 private val en=listOf("qwertyuiop","asdfghjkl","zxcvbnm")
 override fun onCreateInputView():View{box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(4,4,4,4);setBackgroundColor(Color.rgb(235,235,240))};draw();return box}
 private fun draw(){box.removeAllViews()
  val sug=LinearLayout(this).apply{gravity=Gravity.CENTER}
  listOf("שלום","תודה","בוקר טוב").forEach{w->sug.addView(btn(w,1f){commit(w)})};box.addView(sug,row())
  val rows=if(numbers)listOf("1234567890","@#₪%&*()-_+",".,!?/:;") else if(hebrew)he else en
  rows.forEach{s->val r=LinearLayout(this);s.forEach{c->val t=if(!hebrew&&!numbers&&shift)c.uppercase(Locale.US) else c.toString();r.addView(btn(t,1f){commit(t)})};box.addView(r,row())}
  val bottom=LinearLayout(this)
  bottom.addView(btn("⇧",1f){shift=!shift;draw()})
  bottom.addView(btn(if(hebrew)"EN" else "עב",1f){hebrew=!hebrew;numbers=false;shift=false;draw()})
  bottom.addView(btn(if(numbers)"ABC" else "123",1f){numbers=!numbers;draw()})
  bottom.addView(btn("😊",1f){commit("😊")});bottom.addView(btn("⌫",1f){ic()?.deleteSurroundingText(1,0)})
  bottom.addView(btn("↵",1f){enter()});box.addView(bottom,row())
 }
 private fun row()=LinearLayout.LayoutParams(-1,0,1f).apply{setMargins(1,1,1,1)}
 private fun btn(t:String,w:Float,a:()->Unit)=Button(this).apply{text=t;textSize=if(t.length>1)14f else 19f;minWidth=0;minimumWidth=0;setPadding(0,0,0,0);background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=12f};setOnClickListener{a()};layoutParams=LinearLayout.LayoutParams(0,-1,w)}
 private fun ic()=currentInputConnection
 private fun commit(s:String){ic()?.commitText(s,1)}
 private fun enter(){val c=ic()?:return;val action=currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)?:0;if(action!=0&&action!=EditorInfo.IME_ACTION_UNSPECIFIED)c.performEditorAction(action)else c.commitText("\n",1)}
}
