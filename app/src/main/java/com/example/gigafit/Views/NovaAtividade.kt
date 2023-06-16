package com.example.gigafit.Views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gigafit.R
import com.example.gigafit.Views.entities.LocationData
import com.example.gigafit.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class NovaAtividade : AppCompatActivity(), SensorEventListener, LocationListener {

    private val b by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 101

    lateinit var locationData: MutableList<LocationData>
    lateinit var location: LocationData

    private var isRunning = false
    private var isPaused = false
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private val handler = Handler()

    var mSensorManager: SensorManager? = null
    var mAccelerometer: Sensor? = null
    var mLocationManager: LocationManager? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    val maxLocationRecords = 100000
    val locationsRecordeds = 0

    var estadoBotao = false;

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()

                    if (!isRunning) {
                        if (elapsedTime == 0L) {
                            startTime = SystemClock.elapsedRealtime()
                        } else {
                            startTime = SystemClock.elapsedRealtime() - elapsedTime
                        }
                        isRunning = true
                        startTimer()
                    }

                    val btnIniciarParar = findViewById<Button>(R.id.btnIniciarParar)
                    val txtPacotes = findViewById<TextView>(R.id.txtPacotes)
                    val btnPausar = findViewById<ImageView>(R.id.btnPausar)
                    val spinner: Spinner = findViewById(R.id.spinner)

                    val corBtn = Color.parseColor("#d0011c")
                    btnIniciarParar.text = "Parar"
                    btnIniciarParar.setBackgroundColor(corBtn)
                    btnIniciarParar.isEnabled = false
                    btnPausar.isEnabled = true;
                    spinner.isEnabled = false;

                    btnPausar.setColorFilter(resources.getColor(R.color.teal_700))

                    txtPacotes.text = "Iniciado..."
                } else {
                    estadoBotao = !estadoBotao
                    Toast.makeText(this, "Permissão negada", Toast.LENGTH_LONG).show()
                }
            }
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nova_atividade)

        setTitle("Nova Atividade")

        val spinner: Spinner = findViewById(R.id.spinner)

        Spinner(spinner)

        locationData = mutableListOf()

        location = LocationData(0.0, 0.0, System.currentTimeMillis())

        val btnIniciarParar = findViewById<Button>(R.id.btnIniciarParar)
        val btnPausar = findViewById<ImageView>(R.id.btnPausar)

        btnPausar.isEnabled = false;

        var estadoBotaoPausar = false;

        // ACELEROMETRO
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mSensorManager!!.flush(this)
        mSensorManager!!.registerListener(this, mAccelerometer, 16666)

        // GPS
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val txtPacotes = findViewById<TextView>(R.id.txtPacotes)

        btnIniciarParar.setOnClickListener {
            estadoBotao = !estadoBotao

            if (estadoBotao){
                requestLocationPermission()
            }
            else {
                val xmlCriado = CriarXml(locationData)

                if (!xmlCriado) {
                    Toast.makeText(this, "Falha ao criar xml", Toast.LENGTH_SHORT).show()
                }

                elapsedTime = 0
                isRunning = false
                isPaused = false
                updateTimerText(elapsedTime)

                val corBtn = Color.parseColor("#32cd32")
                btnIniciarParar.text = "Iniciar"
                btnIniciarParar.setBackgroundColor(corBtn)
                btnPausar.isEnabled = false;
                estadoBotaoPausar = false;
                spinner.isEnabled = true;

                txtPacotes.text = locationData.count().toString() + " itens salvos"

                locationData = mutableListOf()
            }
        }

        btnPausar.setOnClickListener{
            if (isRunning) {
                val btnPausar = findViewById<ImageView>(R.id.btnPausar)

                if (isPaused) {
                    startTime = SystemClock.elapsedRealtime() - elapsedTime
                    isPaused = false
                    startTimer()

                    startLocationUpdates()

                    mSensorManager?.registerListener(this, mAccelerometer, 16666)

                    btnPausar.setColorFilter(resources.getColor(R.color.teal_700))

                    txtPacotes.text = "Iniciado..."
                } else {
                    elapsedTime = SystemClock.elapsedRealtime() - startTime
                    isPaused = true
                    handler.removeCallbacksAndMessages(null)

                    mLocationManager?.removeUpdates(this)

                    mSensorManager?.unregisterListener(this)

                    btnPausar.setColorFilter(resources.getColor(R.color.laranjapausar))

                    txtPacotes.text = "Pausado"
                }
            }

            estadoBotaoPausar = !estadoBotaoPausar

            btnIniciarParar.isEnabled = estadoBotaoPausar
        }
    }

    override fun onResume() {
        super.onResume()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0f,
                this
            )
        }
    }

    fun updateList() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                locationData.add(location)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                mLocationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    0f,
                    this
                )
            }
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) && ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {

        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    //Coletas do eixo x y z ACELEROMETRO
    override fun onSensorChanged(p0: SensorEvent) {
        //if (ActivityCompat.checkSelfPermission(
                //this,
                //Manifest.permission.ACCESS_FINE_LOCATION
            //) == PackageManager.PERMISSION_GRANTED
            //&& locationsRecordeds <= maxLocationRecords) {

            //location.xaxis = p0.values[0]
            //location.yaxis = p0.values[1]
            //location.zaxis = p0.values[2]
        //}
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    // coletas da latitude e longitude
    override fun onLocationChanged(p0: Location) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && locationsRecordeds <= maxLocationRecords) {
            location.latitude = p0.latitude
            location.longitude = p0.longitude
            location.timestamp = System.currentTimeMillis()

            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    updateList()
                }
            }
        }
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                val currentTime = SystemClock.elapsedRealtime()
                elapsedTime = currentTime - startTime
                updateTimerText(elapsedTime)
                if (isRunning) {
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun updateTimerText(time: Long) {
        val seconds = (time / 1000) % 60
        val minutes = (time / (1000 * 60)) % 60
        val hours = (time / (1000 * 60 * 60)) % 24

        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val timerTextView = findViewById<TextView>(R.id.textViewTempo)
        timerTextView.text = timeString
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        // Implementação do método onStatusChanged
    }

    fun Spinner(spinner: Spinner){
        val opcoes = arrayOf("Caminhar", "Correr", "Bike")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()

                val btnPausar: ImageView = findViewById(R.id.btnPausar)

                var drawable = ContextCompat.getDrawable(this@NovaAtividade, R.drawable.bike)

                if (selectedItem.toString().lowercase().equals("caminhar")) {
                    drawable = ContextCompat.getDrawable(this@NovaAtividade, R.drawable.caminhar)
                }
                else if (selectedItem.toString().lowercase().equals("correr")){
                    drawable = ContextCompat.getDrawable(this@NovaAtividade, R.drawable.correr)
                }

                btnPausar.setImageDrawable(drawable)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(this@NovaAtividade, "Selecione uma atividade física", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun CriarXml(list: MutableList<LocationData>) : Boolean{
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val gpxElement = doc.createElement("gpx")
        gpxElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
        gpxElement.setAttribute("xmlns", "http://www.topografix.com/GPX/1/1")
        gpxElement.setAttribute("xmlns:gpxtpx", "http://www.garmin.com/xmlschemas/TrackPointExtension/v1")
        gpxElement.setAttribute("xmlns:gpxx", "http://www.garmin.com/xmlschemas/GpxExtensions/v3")
        gpxElement.setAttribute("creator", "StravaGPX")
        gpxElement.setAttribute("xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd")
        gpxElement.setAttribute("version", "1.1")
        doc.appendChild(gpxElement)

        val metadataElement = doc.createElement("metadata")
        gpxElement.appendChild(metadataElement)

        val timeElement = doc.createElement("time")

        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val dataFormatadaInicio = formato.format(list[0].timestamp)

        timeElement.appendChild(doc.createTextNode(dataFormatadaInicio))
        metadataElement.appendChild(timeElement)

        val trkElement = doc.createElement("trk")
        gpxElement.appendChild(trkElement)

        val nameElement = doc.createElement("name")
        nameElement.appendChild(doc.createTextNode("GPS"))
        trkElement.appendChild(nameElement)

        val typeElement = doc.createElement("type")
        typeElement.appendChild(doc.createTextNode("1"))
        trkElement.appendChild(typeElement)

        val trksegElement = doc.createElement("trkseg")
        trkElement.appendChild(trksegElement)

        list.forEach { elemento ->
            val trkptElement = doc.createElement("trkpt")
            trkptElement.setAttribute("lat", elemento.latitude.toString())
            trkptElement.setAttribute("lon", elemento.longitude.toString())
            trksegElement.appendChild(trkptElement)

            val dataFormatada = formato.format(elemento.timestamp)

            val trkptTimeElement = doc.createElement("time")
            trkptTimeElement.appendChild(doc.createTextNode(dataFormatada))
            trkptElement.appendChild(trkptTimeElement)
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

        val source = DOMSource(doc)
        val result = StreamResult(System.out)

        transformer.transform(source, result)

        return true
    }
}