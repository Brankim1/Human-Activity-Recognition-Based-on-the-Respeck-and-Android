package com.specknet.pdiotapp.live

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*

import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.*
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.ml.Model
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import kotlinx.android.synthetic.main.activity_me.*
import kotlinx.android.synthetic.main.nav_header.view.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList


class LiveDataActivity : AppCompatActivity() {


//    lateinit var model : Model

    val featureMap = mapOf(
        0 to "Falling",
        1 to "Lying",
        2 to "Running",
        3 to "Sitting or Standing",
        4 to "Walking",
        5 to "Climbing stairs",
        6 to "Descending stairs"
    )


    var inputArray = FloatArray(50 * 6) { 0.toFloat()}

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    var time = 0f
    lateinit var allRespeckData: LineData


    lateinit var respeckChart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)


    lateinit var predictText: TextView
    lateinit var imageView: ImageView
    // Initialise the DrawerLayout, NavigationView and ToggleBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var navView: NavigationView

    private lateinit var database: DatabaseReference
    var uid =""
    var sitTime=0
    var fallingTime=0
    var txtWait=0
    var maxSitTime=10
    var phone="tel:07579919708"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        var user= FirebaseAuth.getInstance().currentUser
        user?.let{
            uid=user.uid.toString()
        }

        getMaxTime()

        setupDrawerLayout()

        setupCharts()

        setupPredictText()

        createNotificationChannel()


        var tem=0.toFloat()
        val runDelayTime = 1000
//run every 1 second to predict and Sitting Reminder and Falling warning
        val handler = Handler()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, runDelayTime.toLong())
                //Sitting Reminder
                if (sitTime>=maxSitTime) {
                    // send Notification every time
                    Toast.makeText(applicationContext, "Sitting too long", Toast.LENGTH_SHORT).show()
                }
                if (sitTime==maxSitTime) {
                    // send Notification once
                    var builder = NotificationCompat.Builder(applicationContext, "chat")
                        .setSmallIcon(R.drawable.icon2)
                        .setContentTitle("Reminder to Move")
                        .setContentText("You are sitting too long, MOVE NOW!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(1, builder.build())
                    }
                }

                //Falling warning
                if (fallingTime==1) {
                    // Falling warning dialog
                    var builder2 = NotificationCompat.Builder(applicationContext, "chat")
                        .setSmallIcon(R.drawable.icon2)
                        .setContentTitle("Falling Detected")
                        .setContentText("Please Choose Yes or No")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(1, builder2.build())
                    }

                    val builder = AlertDialog.Builder(this@LiveDataActivity)
                    builder.setTitle("Falling warning")
                    builder.setMessage("Falling, Are you hurt? Yes to send a message to your emergency contacts or after 10 seconds automatic send")
                    builder.setPositiveButton("YES") { dialog, which ->
                        sendCall()
                    }
                    builder.setNegativeButton("NO") { dialog, which ->
                        Toast.makeText(applicationContext,
                            "I will always company you!", Toast.LENGTH_SHORT).show()
                        fallingTime=0
                        txtWait=0
                    }
                    builder.show()
                    txtWait++
                }
                if(txtWait!=0){
                    if(txtWait==10){
                        sendCall()
                    }else{
                        txtWait++
                    }

                }
                if(inputArray[200]!=tem){//for test whether not connect sensor
                    //predict
                   var predict=updatePredictText()
                    //Sitting Reminder
                    if (predict=="Sitting or Standing"){
                        sitTime++
                    }else{
                        sitTime=0
                    }
                    //Falling warning
                    if (predict=="Falling"){
                        fallingTime=1
                    }else{
                        fallingTime=0
                    }
                    tem=inputArray[200]
                }else {
                    //for test whether not connect sensor
                    predictText.text ="Please connect sensors"
                    imageView.setImageResource(R.drawable.standing)
                }
            }
        }
        handler.postDelayed(runnable, runDelayTime.toLong())

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    val gx = liveData.gyro.x
                    val gy = liveData.gyro.y
                    val gz = liveData.gyro.z

                    time += 1
                    updateGraph("respeck", x, y, z, gx, gy, gz)
                    var inputArrayList = inputArray.drop(6).toMutableList()
                    inputArrayList.addAll(arrayListOf(x, y, z, gx, gy, gz))
                    inputArray = inputArrayList.toFloatArray()

                    uploadRespeckData(x, y, z, gx, gy, gz)
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)


    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "notification"
            val descriptionText = "test_notification_description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("chat", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun setupCharts() {
        respeckChart = findViewById(R.id.respeck_chart)

        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        dataSet_res_accel_x = LineDataSet(entries_res_accel_x, "Accel X")
        dataSet_res_accel_y = LineDataSet(entries_res_accel_y, "Accel Y")
        dataSet_res_accel_z = LineDataSet(entries_res_accel_z, "Accel Z")

        dataSet_res_accel_x.setDrawCircles(false)
        dataSet_res_accel_y.setDrawCircles(false)
        dataSet_res_accel_z.setDrawCircles(false)

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

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(dataSet_res_accel_x)
        dataSetsRes.add(dataSet_res_accel_y)
        dataSetsRes.add(dataSet_res_accel_z)

        allRespeckData = LineData(dataSetsRes)
        respeckChart.data = allRespeckData
        respeckChart.invalidate()


    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float, gx: Float, gy: Float, gz: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "respeck") {
            dataSet_res_accel_x.addEntry(Entry(time, x))
            dataSet_res_accel_y.addEntry(Entry(time, y))
            dataSet_res_accel_z.addEntry(Entry(time, z))

            runOnUiThread {
                allRespeckData.notifyDataChanged()
                respeckChart.notifyDataSetChanged()
                respeckChart.invalidate()
                respeckChart.setVisibleXRangeMaximum(150f)
                respeckChart.moveViewToX(respeckChart.lowestVisibleX + 40)

            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
    }

    // extending func `setupCharts()`
    fun setupPredictText() {
        predictText = findViewById(R.id.predict_text)
        val initInfo = "Initiating..."
        predictText.text = initInfo
        imageView =findViewById(R.id.imagePredict)

    }

    // extending func `updateGraph()`
    fun updatePredictText() : String? {
        // take the first element from the queue
        // and update the graph with it

            val model = Model.newInstance(this)

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 50, 6), DataType.FLOAT32)
//            inputFeature0.loadBuffer(byteBuffer)
            inputFeature0.loadArray(inputArray)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer


            // get feature with max probability
            var max = getMax(outputFeature0.floatArray)

            // display result
            // var s = "Predicting... ($x, $y, $z, $gx, $gy, $gz)"
            var s = featureMap[max] // + "Predicting... ($x, $y, $z, $gx, $gy, $gz)"
            predictText.text = s

            if (s=="Falling"){
                imageView.setImageResource(R.drawable.falling)
            }else if(s=="Lying"){
                imageView.setImageResource(R.drawable.lying)
            }else if(s=="Running"){
                imageView.setImageResource(R.drawable.running)
            }else if(s=="Sitting or Standing"){
                imageView.setImageResource(R.drawable.standing)
            }else if(s=="Walking"){
                imageView.setImageResource(R.drawable.walking)
            }else if(s=="Climbing stairs"){
                imageView.setImageResource(R.drawable.climbing)
            }else if(s=="Descending stairs"){
                imageView.setImageResource(R.drawable.descending)
            }
            model.close()
        return s
    }

    fun getMax(arr:FloatArray) : Int{
        var index = 0
        var max = 0.0f

        for(i in 0..6){
            if(arr[i]>max){
                index = i
                max = arr[i]
            }
        }
        return index

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
                    Toast.makeText(this, "Your are in Predict", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.me -> {
                    val intent = Intent(this, MeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> {
                    false
                }
            }
        }
        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            val intent = Intent(this, ConnectingActivity::class.java)
            startActivity(intent)
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
    fun uploadRespeckData( x: Float, y: Float, z: Float, gx: Float, gy: Float, gz: Float){

        //get time
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("YYYYMMddHHmmssSSS")
        val formatted = current.format(formatter)

        val respeck = Respeck(x, y, z, gx, gy, gz,formatted) as Respeck
        //add to database
        val database =  FirebaseDatabase.getInstance().reference
        database!!.child(uid).child("respeck").child(formatted).setValue(respeck)
    }
    fun getMaxTime(){
        //get Respeck data
        val ref2 = FirebaseDatabase.getInstance().getReference(uid).child("MaxTime")
        val menuListener2 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                   var tem=dataSnapshot.getValue().toString()
                    maxSitTime=tem.toInt()
                }else{
                    maxSitTime=10
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // handle error
            }
        }
        ref2.addListenerForSingleValueEvent(menuListener2)
        val ref3 = FirebaseDatabase.getInstance().getReference(uid).child("phone")
        val menuListener3 = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    phone="tel:"+dataSnapshot.getValue().toString()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // handle error
            }
        }
        ref3.addListenerForSingleValueEvent(menuListener3)
    }
    fun sendCall(){
        try{
            val intent = Intent(Intent.ACTION_CALL);
            intent.data = Uri.parse(phone)
            startActivity(intent)

        }catch(e: Exception){
            Toast.makeText(applicationContext, "Call Permission Failed", Toast.LENGTH_LONG).show()
        }
        fallingTime=0
        txtWait=0
    }
}
