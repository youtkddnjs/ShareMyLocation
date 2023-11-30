package mhha.sample.sharemylocation

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import mhha.sample.sharemylocation.databinding.ActivityLoginBinding

class LoginActivity: AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val callback:(OAuthToken?, Throwable?) -> Unit = { token, error ->
        if(error != null){
            //로그인 실패
            Log.i("LoginActivity", "error == $error")
        }else if(token != null){
            //로그인 성공
            Log.i("LoginActivity", "token == $token")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        KakaoSdk.init(this, "13ef04ff0eb8c75f0ba0dbe7297942c1")

        binding.kakaoTalkLoginButton.setOnClickListener {

            if(UserApiClient.instance.isKakaoTalkLoginAvailable(this)){
                //카카톡 로그인
                UserApiClient.instance.loginWithKakaoTalk(this){ token, error ->
                    if(error != null ){
                        //로그인 실패
                        if(error is ClientError && error.reason == ClientErrorCause.Cancelled){
                            //사용자에 의한 로그인 실패
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    }else if( token != null){
                        //로그인 성공
                        Log.i("LoginActivity", "token == $token")
                    }

                }

            }else{
                //카카오계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)

            }//if(UserApiClient.instance.isKakaoTalkLoginAvailable(this))
        }//binding.kakaoTalkLoginButton.setOnClickListener




    }//override fun onCreate(savedInstanceState: Bundle?)

}//class LoginActivity: AppCompatActivity()