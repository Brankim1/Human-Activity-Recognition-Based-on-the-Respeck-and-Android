package com.specknet.pdiotapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.live.LiveDataActivity

class ResetActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset)
        auth = Firebase.auth
        val textEmail = findViewById<TextInputEditText>(R.id.email3)


        var register = findViewById(R.id.reset) as Button
        register.setOnClickListener {
            val email = textEmail.text.toString()
            if(!email.isEmpty()) {
                auth!!.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // successful!
                            Toast.makeText(
                                baseContext, "Email send successful",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // failed!
                            Toast.makeText(
                                baseContext, "Email send Failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }else{
                Toast.makeText(baseContext, "Input Email",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}