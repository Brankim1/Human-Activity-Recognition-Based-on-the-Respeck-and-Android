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

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = Firebase.auth

        val textEmail = findViewById<TextInputEditText>(R.id.email2)
        val TextPassword = findViewById<TextInputEditText>(R.id.password2)

        var register = findViewById(R.id.register) as Button
        register.setOnClickListener {
            val email = textEmail.text.toString()
            val password = TextPassword.text.toString()
            if(!email.isEmpty()&&!password.isEmpty()){
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            val user = auth.currentUser
                            Toast.makeText(baseContext, "Register successful",
                                Toast.LENGTH_SHORT).show()
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