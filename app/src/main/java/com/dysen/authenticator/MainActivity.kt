package com.dysen.authenticator

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.content.Intent.ACTION_MAIN
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseSetContentView(R.layout.activity_main)

//        transAty("com.dysen.authenticator", "com.dysen.authenticator.AuthenticatorActivity")
//        tv_start.setOnClickListener {//方式1
//            transAty(AuthenticatorActivity::class.java)
//        }
        tv_start.setOnClickListener(this)
    }

    override fun onClick(view: View?) {//方式2
        when (view?.id) {
            R.id.tv_start -> {
                transAty(AuthenticatorActivity::class.java)
            }
        }
    }

//    fun onJump(view: View?) {//方式3
//        when (view?.id) {
//            R.id.tv_start ->{
//                transAty(AuthenticatorActivity::class.java)
//}
//        }
//    }

/**
 * 通过包名和类名 启动Activity
 * @param packageName
 * @param className
 */
fun transAty(packageName: String, className: String) {
    val intent = Intent(ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.setComponent(ComponentName(packageName, className))
    //前名一个参数是应用程序的包名,后一个是这个应用程序的主Activity名
    startActivity(intent)
}

fun transAty(cls: Class<*>) {
    val intent = Intent(this, cls)
    startActivity(intent)
}
}
