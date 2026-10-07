package com.ari900630tech.aikeyboard
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.core.view.setPadding
import androidx.appcompat.app.AppCompatActivity
class MainActivity: AppCompatActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;padding(32)}
  root.addView(TextView(this).apply{text="AI Keyboard";textSize=30f;gravity=Gravity.CENTER})
  root.addView(TextView(this).apply{text="מקלדת מערכת מלאה בעברית ובאנגלית";textSize=18f;gravity=Gravity.CENTER;setPadding(20,20,20,20)})
  root.addView(Button(this).apply{text="הפעלת המקלדת";setOnClickListener{startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))}})
  root.addView(TextView(this).apply{text="לאחר ההפעלה בחר ב‑AI Keyboard כמקלדת ברירת המחדל.";gravity=Gravity.CENTER;setPadding(24,24,24,24)})
  setContentView(root)
 }
}
