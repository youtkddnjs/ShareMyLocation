package mhha.sample.sharemylocation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import mhha.sample.sharemylocation.databinding.ActivityLoginBinding

class LoginActivity: AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var emailLoginResult: ActivityResultLauncher<Intent>
    private lateinit var pandingUser: User

    private val callback:(OAuthToken?, Throwable?) -> Unit = { token, error ->
        if(error != null){
            //로그인 실패
            showErrorToast()
            Log.i("LoginActivity", "error == $error")
            error.printStackTrace()
        }else if(token != null){
            //로그인 성공
            getKakaoAccountInfo()
            Log.i("LoginActivity", "token == $token")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        KakaoSdk.init(this, "13ef04ff0eb8c75f0ba0dbe7297942c1")

        emailLoginResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == RESULT_OK){
                val email = it.data?.getStringExtra("email")

                if(email == null){
                    showErrorToast()
                    return@registerForActivityResult
                }else{
                    signInFirebase(pandingUser ,email)
                }
            }
        }

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
                        if(Firebase.auth.currentUser == null){
                            //카카오톡에서 정보를 가져와서 파이어베이스 로그인
                            getKakaoAccountInfo()
                        }else{
                            //로그인 성공
                            Log.i("LoginActivity", "token == $token")
                            navigateToMapActivity()
                        }
                    }
                }
            }else{
                //카카오계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }//if(UserApiClient.instance.isKakaoTalkLoginAvailable(this))
        }//binding.kakaoTalkLoginButton.setOnClickListener
    }//override fun onCreate(savedInstanceState: Bundle?)

    private fun navigateToMapActivity(){
        startActivity(Intent(this,MapActivity::class.java))
    }//private fun navigateToMapActivity()

    private fun getKakaoAccountInfo(){
        UserApiClient.instance.me { user, error ->
            if(error != null){
                showErrorToast()
                Log.e("LoginActivity", "getKakaoAccountInfo :: fail $error")
                error.printStackTrace()
            }else if(user != null){
                Log.d("LoginActivity", "user : 회원번호 : ${user.id} / 이메일 : ${user.kakaoAccount?.email} / 닉네임 : ${user.kakaoAccount?.profile?.nickname} / 프로필 사진 : ${user.kakaoAccount?.profile?.thumbnailImageUrl}")
                checkKakaoUserData(user)
            }
        }//UserApiClient.instance.me
    }//private fun getKakaoAccountInfo()

    private fun checkKakaoUserData(user: User){
        var kakaoEmail = user.kakaoAccount?.email.orEmpty()
        if(kakaoEmail.isEmpty()){
            Toast.makeText(this,"카카오 이메일이 없음",Toast.LENGTH_SHORT).show()
            pandingUser = user
            emailLoginResult.launch(Intent(this,EmailLoginActivity::class.java))
            return
        }
        signInFirebase(user, kakaoEmail)
    }//private fun checkKakaoUserData(user: User)

    private fun signInFirebase(user: User, email: String) {
        val uId = user.id.toString()
        Firebase.auth.createUserWithEmailAndPassword(email, uId)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    //다음 과정으로 넘어 갈 것.
                    updateFirebaseDatabase(user)
                }else{
                    showErrorToast()
                }
            }.addOnFailureListener {
                // 이미 가입된 계정
                if (it is FirebaseAuthUserCollisionException) {
                    Firebase.auth.signInWithEmailAndPassword(email, uId).addOnCompleteListener { result ->
                        if(result.isSuccessful){
                            //다음 과정으로 넘어 갈 것.
                            updateFirebaseDatabase(user)
                        }else{
                            showErrorToast()
                        }
                    }.addOnFailureListener { error ->
                        error.printStackTrace()
                        showErrorToast()
                    }//addOnFailureListener
                } else{
                    showErrorToast()
                }
            }//addOnFailureListener
    }//private fun signInFirebase(user: User, email: String)

    private fun updateFirebaseDatabase(user: User){
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        val userMap = mutableMapOf<String,Any>()
        userMap["uid"] = uid
        userMap["name"] = user.kakaoAccount?.profile?.nickname.orEmpty()
        userMap["profilePhoto"] = user.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()

        Firebase.database.reference.child("User").child(uid).updateChildren(userMap)
        navigateToMapActivity()
    }//private fun updateFirebaseDatabase(user: User)


    private fun showErrorToast(){
            Toast.makeText(this, "Login Fail", Toast.LENGTH_SHORT).show()
    }

}//class LoginActivity: AppCompatActivity()