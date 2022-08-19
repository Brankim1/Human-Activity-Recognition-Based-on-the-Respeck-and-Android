package com.specknet.pdiotapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.live.LiveDataActivity
import kotlinx.android.synthetic.main.nav_header.view.*

class MeActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarToggle: ActionBarDrawerToggle
    lateinit var navView: NavigationView
    private lateinit var auth: FirebaseAuth
    var uid =""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_me)
        setupDrawerLayout()
        val maxTimeTxt = findViewById<TextInputEditText>(R.id.maxTime)
        val phone = findViewById<TextInputEditText>(R.id.phone)
        var register = findViewById(R.id.maxTimeButton) as Button
        var phoneButton = findViewById(R.id.phoneButton) as Button

        auth = Firebase.auth
        var user=FirebaseAuth.getInstance().currentUser
        var emailAccount =""
        user?.let{
            emailAccount=user.email.toString()
            uid=user.uid.toString()
        }
        val email = findViewById<TextView>(R.id.email4) as TextView
        email.text=emailAccount
        var logout = findViewById(R.id.logout) as Button
        logout.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this, LoginAvtivity::class.java)
            startActivity(intent)
            finish()
        }
        register.setOnClickListener {
            val tem = maxTimeTxt.text.toString()
            var maxTime=tem.toInt()*60
            if (!tem.isEmpty()){
                //add to database
                val database =  FirebaseDatabase.getInstance().reference
                database!!.child(uid).child("MaxTime").setValue(maxTime)
                Toast.makeText(
                    baseContext, "Update successful",
                    Toast.LENGTH_SHORT
                ).show()
                maxTimeTxt.setText("")
            }else{
                Toast.makeText(
                    baseContext, "Empty input",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
        phoneButton.setOnClickListener {
            val tem = phone.text.toString()
            if (!tem.isEmpty()){
                //add to database
                val database =  FirebaseDatabase.getInstance().reference
                database!!.child(uid).child("phone").setValue(tem)
                Toast.makeText(
                    baseContext, "Update successful",
                    Toast.LENGTH_SHORT
                ).show()
                maxTimeTxt.setText("")
            }else{
                Toast.makeText(
                    baseContext, "Empty input",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
        //get maxtime
        //get Respeck data
        val ref2 = FirebaseDatabase.getInstance().getReference(uid).child("MaxTime")
        val menuListener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    var tem=dataSnapshot.getValue().toString()
                    var time=((tem.toInt())/60).toString()
                    maxTimeTxt.setText(time)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // handle error
            }
        }
        ref2.addListenerForSingleValueEvent(menuListener2)
        //get phone
        val ref3 = FirebaseDatabase.getInstance().getReference(uid).child("phone")
        val menuListener3 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    var phonenum=dataSnapshot.getValue().toString()
                    phone.setText(phonenum)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // handle error
            }
        }
        ref3.addListenerForSingleValueEvent(menuListener3)
    }
    fun setupDrawerLayout(){
        // Call findViewById on the DrawerLayout

        drawerLayout = findViewById(R.id.drawerLayout)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)
        toolbar?.navigationIcon = ContextCompat.getDrawable(this,R.drawable.ic_menu_black_24dp)
        //toolbar button set listener
        toolbar?.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Pass the ActionBarToggle action into the drawerListener
        actionBarToggle = ActionBarDrawerToggle(this, drawerLayout, 0, 0)
        drawerLayout.addDrawerListener(actionBarToggle)

        // Display the hamburger icon to launch the drawer
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Call syncState() on the action bar so it'll automatically change to the back button when the drawer layout is open
        actionBarToggle.syncState()


        // Call findViewById on the NavigationView
        navView = findViewById(R.id.navView)

        // Call setNavigationItemSelectedListener on the NavigationView to detect when items are clicked
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.predict -> {
                    val intent = Intent(this, LiveDataActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.me -> {
                    Toast.makeText(this, "Your are in Me", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> {
                    false
                }
            }
        }
        // add email to it
        var auth: FirebaseAuth
        auth = Firebase.auth
        var user=FirebaseAuth.getInstance().currentUser
        var emailAccount =""
        user?.let{
            emailAccount=user.email.toString()
        }
        val headerView: View = navView.getHeaderView(0)
        headerView.emailheader.text=emailAccount

        }
    }
