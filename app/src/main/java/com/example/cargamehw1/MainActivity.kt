package com.example.cargamehw1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // אינדקס המסלול הנוכחי של המכונית (0 = שמאל, 1 = אמצע, 2 = ימין)
    private var currentLane = 1
    private lateinit var cars: Array<ImageView>

    // משתני מנגנון המכשולים
    private lateinit var obstacles: Array<ImageView>
    private var activeObstacleLane = 1
    private var obstacleYPosition = 0f

    // מערך שיחזיק את שלושת הלבבות מהמסך וכמה חיים נשארו
    private lateinit var hearts: Array<ImageView>
    private var lives = 3

    // משתני הטיימר
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gameTicker: Runnable
    private val delayMillis: Long = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. קישור כפתורים, מכוניות ומכשולים
        val btnLeft = findViewById<Button>(R.id.main_BTN_left)
        val btnRight = findViewById<Button>(R.id.main_BTN_right)

        cars = arrayOf(
            findViewById(R.id.main_IMG_car1),
            findViewById(R.id.main_IMG_car2),
            findViewById(R.id.main_IMG_car3)
        )

        obstacles = arrayOf(
            findViewById(R.id.main_IMG_obstacle1),
            findViewById(R.id.main_IMG_obstacle2),
            findViewById(R.id.main_IMG_obstacle3)
        )

        // 2. קישור הלבבות מה-XML למערך
        hearts = arrayOf(
            findViewById(R.id.main_IMG_heart1),
            findViewById(R.id.main_IMG_heart2),
            findViewById(R.id.main_IMG_heart3)
        )

        updateCarVisibility()
        generateNewObstacle()

        // 3. לולאת הטיימר של המשחק
        gameTicker = Runnable {
            moveObstacleDown()
            checkCollision()
            handler.postDelayed(gameTicker, delayMillis)
        }

        // 4. האזנה ללחצנים
        btnLeft.setOnClickListener {
            if (currentLane > 0) {
                currentLane--
                updateCarVisibility()
            }
        }

        btnRight.setOnClickListener {
            if (currentLane < 2) {
                currentLane++
                updateCarVisibility()
            }
        }
    }

    // פונקציה שמורידה את המכשול הנוכחי למטה
    private fun moveObstacleDown() {
        obstacleYPosition += 50f
        obstacles[activeObstacleLane].translationY = obstacleYPosition

        if (obstacleYPosition > 1550f) {
            resetObstacle()
            generateNewObstacle()
        }
    }

    // פונקציה שבודקת האם המכשול פגע במכונית ברגע זה
    private fun checkCollision() {
        if (activeObstacleLane == currentLane && obstacleYPosition >= 1300f && obstacleYPosition <= 1550f) {
            handleCrash()
            resetObstacle()
            generateNewObstacle()
        }
    }

    // פונקציה שמטפלת בהורדת חיים, אפקטים והודעות משודרגות
    private fun handleCrash() {
        if (lives <= 0) return

        lives-- // הורדת חיים ב-1

        try {
            // העלמת הלב המתאים מהמסך
            if (lives in hearts.indices) {
                hearts[lives].visibility = View.INVISIBLE
            }

            // הפעלת רטט בטלפון
            vibratePhone()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // בדיקה: האם נגמרו כל החיים?
        if (lives == 0) {
            // שיפור 2: מציג רק את הודעת סוף המשחק, בלי ההודעה של ה-0 חיים!
            showLargeTopToast("המשחק נגמר ! מתחילים מחדש!")
            resetGame()
        } else {
            // אם עדיין יש חיים - מציג את הודעת הפגיעה הרגילה בחלק העליון
            showLargeTopToast("אאוץ'!!! נשארו לך עוד $lives חיים")
        }
    }

    // שיפור 1: פונקציה חכמה שמייצרת הודעה גדולה ומקפיצה אותה לחצי העליון של המסך
    private fun showLargeTopToast(message: String) {
        // יצירת ה-Toast הבסיסי
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)

        // קובע שההודעה תופיע בחצי העליון של המסך (TOP) עם היסט קל למטה (yOffset = 250)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 250)

        // טריק חכם: גישה לטקסט הפנימי של ה-Toast כדי להגדיל את הגופן שלו
        try {
            val toastView = toast.view
            val toastTextView = toastView?.findViewById<TextView>(android.R.id.message)
            toastTextView?.textSize = 24f // הגדלת גודל הגופן ל-24 (בולט וברור!)
        } catch (e: Exception) {
            // גיבוי למקרה שבגרסת אנדרואיד מסוימת העיצוב הפנימי שונה
            e.printStackTrace()
        }

        toast.show() // הצגת ההודעה
    }

    // פונקציה שמפעילה את הרוטט
    private fun vibratePhone() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // פונקציה שמחזירה את המשחק למצב התחלתי כשנפסלים
    private fun resetGame() {
        lives = 3
        // החזרת כל הלבבות להיות גלויים
        for (heart in hearts) {
            heart.visibility = View.VISIBLE
        }
        // החזרת המכונית למרכז
        currentLane = 1
        updateCarVisibility()
    }

    private fun resetObstacle() {
        obstacles[activeObstacleLane].visibility = View.INVISIBLE
        obstacles[activeObstacleLane].translationY = 0f
        obstacleYPosition = 0f
    }

    private fun generateNewObstacle() {
        activeObstacleLane = Random.nextInt(3)
        obstacles[activeObstacleLane].visibility = View.VISIBLE
    }

    private fun updateCarVisibility() {
        for (i in cars.indices) {
            if (i == currentLane) {
                cars[i].visibility = View.VISIBLE
            } else {
                cars[i].visibility = View.INVISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(gameTicker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(gameTicker)
    }
}