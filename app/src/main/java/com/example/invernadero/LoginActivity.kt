package com.example.invernadero

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etCorreo: EditText
    private lateinit var etClave: EditText
    private lateinit var btnIngresar: Button
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            irAlMenu()
            return
        }

        etCorreo = findViewById(R.id.etCorreo)
        etClave = findViewById(R.id.etClave)
        btnIngresar = findViewById(R.id.btnIngresar)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        btnIngresar.setOnClickListener { iniciarSesion() }
        btnRegistrar.setOnClickListener { registrarUsuario() }
    }

    private fun datosValidos(correo: String, clave: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo válido"
            return false
        }
        if (clave.length < 6) {
            etClave.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        return true
    }

    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val clave = etClave.text.toString()
        if (!datosValidos(correo, clave)) return

        btnIngresar.isEnabled = false
        auth.signInWithEmailAndPassword(correo, clave)
            .addOnCompleteListener { tarea ->
                btnIngresar.isEnabled = true
                if (tarea.isSuccessful) {
                    irAlMenu()
                } else {
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registrarUsuario() {
        val correo = etCorreo.text.toString().trim()
        val clave = etClave.text.toString()
        if (!datosValidos(correo, clave)) return

        btnRegistrar.isEnabled = false
        auth.createUserWithEmailAndPassword(correo, clave)
            .addOnCompleteListener { tarea ->
                btnRegistrar.isEnabled = true
                if (tarea.isSuccessful) {
                    Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show()
                    irAlMenu()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo crear la cuenta: ${tarea.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun irAlMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }
}