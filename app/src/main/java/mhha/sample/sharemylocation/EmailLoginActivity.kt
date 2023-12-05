package mhha.sample.sharemylocation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import mhha.sample.sharemylocation.databinding.ActivityEmailloginBinding

class EmailLoginActivity: AppCompatActivity() {

    private lateinit var binding: ActivityEmailloginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailloginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.doneButton.setOnClickListener {
            if(binding.emailEditText.text.isNotEmpty()){
                val data = Intent().apply {
                    putExtra("email",binding.emailEditText.text.toString())
                }
                setResult(RESULT_OK, data)
                finish()
            }else{
                Toast.makeText(this,"이메일 입력 요망", Toast.LENGTH_SHORT).show()
            }
        }

    }//override fun onCreate(savedInstanceState: Bundle?)



}//class EmailLoginActivity: AppCompatActivity()