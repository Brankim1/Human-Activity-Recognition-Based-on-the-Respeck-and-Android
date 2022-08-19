package com.specknet.pdiotapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.live.LiveDataActivity

class LoginAvtivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_avtivity)

        // Initialize Firebase Auth
        auth = Firebase.auth
        // Check if user is signed in (non-null)
        val currentUser = auth.currentUser
        if(currentUser != null){
            val intent = Intent(this, LiveDataActivity::class.java)
            startActivity(intent)
            finish()
        }
        val register = findViewById<TextView>(R.id.registerText) as TextView
        register.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)

        }
        val forget = findViewById<TextView>(R.id.forgetText) as TextView
        forget.setOnClickListener(){
            val intent = Intent(this, ResetActivity::class.java)
            startActivity(intent)
        }

        val textEmail = findViewById<TextInputEditText>(R.id.email)
        val TextPassword = findViewById<TextInputEditText>(R.id.password)

        var login = findViewById(R.id.login) as Button
        login.setOnClickListener {
            val email = textEmail.text.toString()
            val password = TextPassword.text.toString()
            if (!email.isEmpty()&&!password.isEmpty()){
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val intent = Intent(this, LiveDataActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }else{
                Toast.makeText(baseContext, "Input Email or Password",
                    Toast.LENGTH_SHORT).show()
            }

        }

    }
}