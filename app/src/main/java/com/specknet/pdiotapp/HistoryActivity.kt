package com.specknet.pdiotapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.live.LiveDataActivity
import kotlinx.android.synthetic.main.nav_header.view.*

class HistoryActivity : AppCompatActivity() {

    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarToggle: ActionBarDrawerToggle
    lateinit var navView: NavigationView

    val thingyX : MutableList<Float> = arrayListOf()
    val thingyY : MutableList<Float> = arrayListOf()
    val thingyZ : MutableList<Float> = arrayListOf()
    val thingyGX : MutableList<Float> = arrayListOf()
    val thingyGY : MutableList<Float> = arrayListOf()
    val thingyGZ : MutableList<Float> = arrayListOf()
    val thingyTime : MutableList<String> = arrayListOf()

    val respeckX : MutableList<Float> = arrayListOf()
    val respeckY : MutableList<Float> = arrayListOf()
    val respeckZ : MutableList<Float> = arrayListOf()
    val respeckGX : MutableList<Float> = arrayListOf()
    val respeckGY : MutableList<Float> = arrayListOf()
    val respeckGZ : MutableList<Float> = arrayListOf()
    val respeckTime : MutableList<String> = arrayListOf()

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet
    lateinit var dataSet_res_accel_Gx: LineDataSet
    lateinit var dataSet_res_accel_Gy: LineDataSet
    lateinit var dataSet_res_accel_Gz: LineDataSet
    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet
    lateinit var dataSet_thingy_accel_Gx: LineDataSet
    lateinit var dataSet_thingy_accel_Gy: LineDataSet
    lateinit var dataSet_thingy_accel_Gz: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData

    lateinit var allRespeckData2: LineData

    lateinit var respeckChart: LineChart
    lateinit var respeckChart2: LineChart
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        getHisData()
        setupDrawerLayout()
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
                    Toast.makeText(this, "Your are in History", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.me -> {
                    val intent = Intent(this, MeActivity::class.java)
                    startActivity(intent)
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
    fun getHisData(){
        var user= FirebaseAuth.getInstance().currentUser
        var uid =""
        user?.let{
            uid=user.uid.toString()
        }

        //get Respeck data
        val ref2 = FirebaseDatabase.getInstance().getReference(uid).child("respeck")
        val menuListener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    val respeck = child.getValue(Respeck::class.java)
                    respeckX.add(respeck!!.getX())
                    respeckY.add(respeck!!.getY())
                    respeckZ.add(respeck!!.getZ())
                    respeckGX.add(respeck!!.getGX())
                    respeckGY.add(respeck!!.getGY())
                    respeckGZ.add(respeck!!.getGZ())
                    respeckTime.add(respeck!!.getTime())
                }
                drawRespeckChart()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // handle error
            }
        }

        ref2.addListenerForSingleValueEvent(menuListener2)
    }
    fun drawRespeckChart(){
        respeckChart = findViewById(R.id.respeck_history_chart)
        respeckChart2 = findViewById(R.id.thingy_history_chart)
        // Respeck
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()
        val entries_res_accel_Gx = ArrayList<Entry>()
        val entries_res_accel_Gy = ArrayList<Entry>()
        val entries_res_accel_Gz = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")
        dataSet_res_accel_Gx = LineDataSet(entries_res_accel_Gx, "Gyroscope X")
        dataSet_res_accel_Gy = LineDataSet(entries_res_accel_Gy, "Gyroscope Y")
        dataSet_res_accel_Gz = LineDataSet(entries_res_accel_Gz, "Gyroscope Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)
        dataSet_res_accel_Gx.setDrawCircles(false)
        dataSet_res_accel_Gy.setDrawCircles(false)
        dataSet_res_accel_Gz.setDrawCircles(false)

        dataSet_res_accel_x.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        dataSet_res_accel_y.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        dataSet_res_accel_z.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )
        dataSet_res_accel_Gx.setColor(
            ContextCompat.getColor(
                this,
                R.color.orange
            )
        )
        dataSet_res_accel_Gy.setColor(
            ContextCompat.getColor(
                this,
                R.color.purple
            )
        )
        dataSet_res_accel_Gz.setColor(
            ContextCompat.getColor(
                this,
                R.color.pink
            )
        )
        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)
        val dataSetsRes2 = ArrayList<ILineDataSet>()
        dataSetsRes2.add(dataSet_res_accel_Gx)
        dataSetsRes2.add(dataSet_res_accel_Gy)
        dataSetsRes2.add(dataSet_res_accel_Gz)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()
        allRespeckData2 = LineData(dataSetsRes2)
        respeckChart2.data = allRespeckData2
        respeckChart2.invalidate()
        var time = 0f
        for (i in 0..respeckX.size-1){
            dataSet_res_accel_x.addEntry(Entry(time, respeckX[i]))
            dataSet_res_accel_y.addEntry(Entry(time, respeckY[i]))
            dataSet_res_accel_z.addEntry(Entry(time, respeckZ[i]))
            dataSet_res_accel_Gx.addEntry(Entry(time, respeckGX[i]))
            dataSet_res_accel_Gy.addEntry(Entry(time, respeckGY[i]))
            dataSet_res_accel_Gz.addEntry(Entry(time, respeckGZ[i]))
            time+=1
        }
        allRespeckData.notifyDataChanged()
        respeckChart.notifyDataSetChanged()
        respeckChart.invalidate()
        respeckChart.setVisibleXRangeMaximum(150f)
        respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)
        allRespeckData2.notifyDataChanged()
        respeckChart2.notifyDataSetChanged()
        respeckChart2.invalidate()
        respeckChart2.setVisibleXRangeMaximum(150f)
        respeckChart2.moveViewToX(respeckChart2.lowestVisibleX + 40)

    }

}