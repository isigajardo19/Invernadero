package com.example.invernadero

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductorActivity : AppCompatActivity() {

    private val urlBaseDeDatos = "https://invernadero-4d8ad-default-rtdb.firebaseio.com"

    private lateinit var refEstado: DatabaseReference
    private lateinit var consultaHistorial: Query

    private lateinit var tvTemperatura: TextView
    private lateinit var tvAlerta: TextView
    private lateinit var tvVentilacion: TextView
    private lateinit var tvRiego: TextView
    private lateinit var tvModo: TextView
    private lateinit var tvHistorial: TextView
    private lateinit var etUmbral: EditText

    // Último estado conocido del invernadero
    private var temperatura: Double? = null
    private var umbral = 30.0
    private var modo = "automatico"
    private var ventilacion = false
    private var riego = false

    private val escuchadorEstado = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) {
                tvAlerta.text = "Aún no hay datos. Abre el invernadero en otro dispositivo."
                return
            }
            temperatura = (snapshot.child("temperatura").value as? Number)?.toDouble()
            umbral = (snapshot.child("umbral").value as? Number)?.toDouble() ?: umbral
            modo = snapshot.child("modo").getValue(String::class.java) ?: modo
            ventilacion = snapshot.child("ventilacion").getValue(Boolean::class.java) ?: false
            riego = snapshot.child("riego").getValue(Boolean::class.java) ?: false
            mostrarEnPantalla()
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(
                this@ProductorActivity,
                "Error de conexión: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val escuchadorHistorial = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val formato = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val lineas = mutableListOf<String>()
            for (lectura in snapshot.children) {
                val temp = (lectura.child("temperatura").value as? Number)?.toDouble() ?: continue
                val momento = (lectura.child("timestamp").value as? Number)?.toLong() ?: 0L
                lineas.add(String.format("%s  →  %.1f °C", formato.format(Date(momento)), temp))
            }
            tvHistorial.text = if (lineas.isEmpty()) {
                "Sin lecturas todavía"
            } else {
                lineas.reversed().joinToString("\n") // la más reciente arriba
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productor)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        val refUsuario = FirebaseDatabase.getInstance(urlBaseDeDatos)
            .getReference("invernaderos")
            .child(uid)
        refEstado = refUsuario.child("estado")
        consultaHistorial = refUsuario.child("historial").limitToLast(10)

        tvTemperatura = findViewById(R.id.tvTemperatura)
        tvAlerta = findViewById(R.id.tvAlerta)
        tvVentilacion = findViewById(R.id.tvVentilacion)
        tvRiego = findViewById(R.id.tvRiego)
        tvModo = findViewById(R.id.tvModo)
        tvHistorial = findViewById(R.id.tvHistorial)
        etUmbral = findViewById(R.id.etUmbral)

        findViewById<Button>(R.id.btnGuardarUmbral).setOnClickListener { guardarUmbral() }
        findViewById<Button>(R.id.btnModo).setOnClickListener { cambiarModo() }
        findViewById<Button>(R.id.btnVentilacion).setOnClickListener { alternarVentilacion() }
        findViewById<Button>(R.id.btnRiego).setOnClickListener { alternarRiego() }
        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        if (::refEstado.isInitialized) {
            refEstado.addValueEventListener(escuchadorEstado)
            consultaHistorial.addValueEventListener(escuchadorHistorial)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::refEstado.isInitialized) {
            refEstado.removeEventListener(escuchadorEstado)
            consultaHistorial.removeEventListener(escuchadorHistorial)
        }
    }

    private fun guardarUmbral() {
        val valor = etUmbral.text.toString().trim().toDoubleOrNull()
        if (valor == null || valor < 0 || valor > 60) {
            etUmbral.error = "Escribe un número entre 0 y 60"
            return
        }
        refEstado.child("umbral").setValue(valor)
        etUmbral.text.clear()
        Toast.makeText(this, "Umbral guardado: $valor °C", Toast.LENGTH_SHORT).show()
    }

    private fun cambiarModo() {
        val nuevoModo = if (modo == "automatico") "manual" else "automatico"
        refEstado.child("modo").setValue(nuevoModo)
    }

    private fun alternarVentilacion() {
        if (modo != "manual") {
            Toast.makeText(this, "Cambia a modo manual para controlarla", Toast.LENGTH_SHORT).show()
            return
        }
        refEstado.child("ventilacion").setValue(!ventilacion)
    }

    private fun alternarRiego() {
        if (modo != "manual") {
            Toast.makeText(this, "Cambia a modo manual para controlarlo", Toast.LENGTH_SHORT).show()
            return
        }
        refEstado.child("riego").setValue(!riego)
    }

    private fun mostrarEnPantalla() {
        val temp = temperatura
        tvTemperatura.text = if (temp != null) String.format("%.1f °C", temp) else "-- °C"

        if (temp != null && temp > umbral) {
            tvAlerta.text = String.format("⚠ ALERTA: pasó el umbral de %.1f °C", umbral)
            tvAlerta.setTextColor(Color.parseColor("#C62828"))
        } else {
            tvAlerta.text = String.format("✔ Temperatura normal (umbral %.1f °C)", umbral)
            tvAlerta.setTextColor(Color.parseColor("#2E7D32"))
        }

        tvVentilacion.text = "Ventilación: " + if (ventilacion) "ENCENDIDA" else "apagada"
        tvRiego.text = "Riego: " + if (riego) "ENCENDIDO" else "apagado"
        tvModo.text = "Modo: $modo"
    }
}
