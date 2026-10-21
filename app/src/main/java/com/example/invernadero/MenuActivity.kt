package com.example.invernadero

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        auth = FirebaseAuth.getInstance()

        val usuario = auth.currentUser
        if (usuario == null) {
            cerrarSesion()
            return
        }

        findViewById<TextView>(R.id.tvBienvenida).text = "Bienvenido, ${usuario.email}"

        findViewById<Button>(R.id.btnProductor).setOnClickListener {
            abrirPantalla(ProductorActivity::class.java)
        }
        findViewById<Button>(R.id.btnInvernadero).setOnClickListener {
            abrirPantalla(InvernaderoActivity::class.java)
        }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }
    }


    private fun abrirPantalla(destino: Class<*>) {
        startActivity(Intent(this, destino))
    }

    private fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}