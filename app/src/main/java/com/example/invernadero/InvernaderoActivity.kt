package com.example.invernadero

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class InvernaderoActivity : AppCompatActivity() {

    private val urlBaseDeDatos = "https://invernadero-4d8ad-default-rtdb.firebaseio.com"

    private lateinit var refUsuario: DatabaseReference
    private lateinit var refEstado: DatabaseReference

    private lateinit var tvTemperatura: TextView
    private lateinit var tvUmbral: TextView
    private lateinit var tvModo: TextView
    private lateinit var tvVentilacion: TextView
    private lateinit var tvRiego: TextView

    private var temperatura = 25.0
    private var umbral = 30.0
    private var modo = "automatico"
    private var ventilacion = false
    private var riego = false

    private val escuchador = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) {
                crearEstadoInicial()
                return
            }
            temperatura = (snapshot.child("temperatura").value as? Number)?.toDouble() ?: temperatura
            umbral = (snapshot.child("umbral").value as? Number)?.toDouble() ?: umbral
            modo = snapshot.child("modo").getValue(String::class.java) ?: modo
            ventilacion = snapshot.child("ventilacion").getValue(Boolean::class.java) ?: false
            riego = snapshot.child("riego").getValue(Boolean::class.java) ?: false

            mostrarEnPantalla()
            aplicarReglaAutomatica()
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(
                this@InvernaderoActivity,
                "Error de conexión: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invernadero)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        refUsuario = FirebaseDatabase.getInstance(urlBaseDeDatos)
            .getReference("invernaderos")
            .child(uid)
        refEstado = refUsuario.child("estado")

        tvTemperatura = findViewById(R.id.tvTemperatura)
        tvUmbral = findViewById(R.id.tvUmbral)
        tvModo = findViewById(R.id.tvModo)
        tvVentilacion = findViewById(R.id.tvVentilacion)
        tvRiego = findViewById(R.id.tvRiego)

        findViewById<Button>(R.id.btnSubir).setOnClickListener { cambiarTemperatura(1.0) }
        findViewById<Button>(R.id.btnBajar).setOnClickListener { cambiarTemperatura(-1.0) }
        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        if (::refEstado.isInitialized) refEstado.addValueEventListener(escuchador)
    }

    override fun onStop() {
        super.onStop()
        if (::refEstado.isInitialized) refEstado.removeEventListener(escuchador)
    }

    private fun crearEstadoInicial() {
        refEstado.setValue(
            mapOf(
                "temperatura" to 25.0,
                "umbral" to 30.0,
                "modo" to "automatico",
                "ventilacion" to false,
                "riego" to false,
                "timestamp" to ServerValue.TIMESTAMP
            )
        )
    }

    private fun cambiarTemperatura(cambio: Double) {
        temperatura += cambio
        guardarLectura()
    }

    private fun guardarLectura() {
        val cambios = mutableMapOf<String, Any>(
            "temperatura" to temperatura,
            "timestamp" to ServerValue.TIMESTAMP
        )
        if (modo == "automatico") {
            val activar = temperatura > umbral
            cambios["ventilacion"] = activar
            cambios["riego"] = activar
        }
        refEstado.updateChildren(cambios)

        refUsuario.child("historial").push().setValue(
            mapOf(
                "temperatura" to temperatura,
                "timestamp" to ServerValue.TIMESTAMP
            )
        )
    }

    private fun aplicarReglaAutomatica() {
        if (modo != "automatico") return
        val activar = temperatura > umbral
        if (ventilacion != activar || riego != activar) {
            refEstado.updateChildren(mapOf("ventilacion" to activar, "riego" to activar))
        }
    }

    private fun mostrarEnPantalla() {
        tvTemperatura.text = String.format("%.1f °C", temperatura)
        tvUmbral.text = String.format("Umbral: %.1f °C", umbral)
        tvModo.text = "Modo: $modo"
        tvVentilacion.text = "Ventilación: " + if (ventilacion) "ENCENDIDA" else "apagada"
        tvRiego.text = "Riego: " + if (riego) "ENCENDIDO" else "apagado"
    }
}