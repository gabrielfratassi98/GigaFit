package com.example.gigafit.Views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.gigafit.R
import com.example.gigafit.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val b by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setTitle("Home")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuNovaAtv){
            val intent = Intent(this, NovaAtividade::class.java)
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }
}