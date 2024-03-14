package com.heyday.media_player

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.heyday.pos.mylibrary.iam.http.Login
import com.heyday.pos.mylibrary.notify.http.Channel
import com.heyday.pos.mylibrary.service_package.ServiceGlobal
import com.heyday.pos.mylibrary.service_package.util.*
import com.heyday.pos.mylibrary.service_package.widget.DelayedProgressDialog
import kotlinx.coroutines.*
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec

class AuthenticationActivity : AppCompatActivity() {
    private lateinit var cursor: Cursor
    private lateinit var qrCode: ImageView
    private lateinit var snCode: ImageView
    private lateinit var textViewSn: TextView
    private lateinit var db: SQLiteDatabase
    private lateinit var layout: LinearLayout
    private lateinit var context: Context
    private val coroutineScope = CoroutineScope(
        Dispatchers.IO
    )

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_authentication)
        context = this
        DelayedProgressDialog.getInstance().show(
            supportFragmentManager,
            "tag"
        )
        val dbHelper = DatabaseUtil(this)
        db = dbHelper.writableDatabase
        qrCode =
                findViewById(R.id.qr_code_authentication)
        snCode = findViewById(R.id.qr_code_sn)
        textViewSn = findViewById(R.id.tv_sn)
        layout = findViewById(R.id.view1)
        layout.visibility = View.GONE
        // 首先查询是否已经存在数据
        val query =
                "SELECT * FROM authentication_table"
        cursor = db.rawQuery(query, null)
        try {
            coroutineScope.launch {
                if (cursor.moveToFirst()) {
                    val sn = cursor.getString(
                        cursor.getColumnIndex("sn")
                    )
                    val privateKeyString =
                            cursor.getString(
                                cursor.getColumnIndex("private_key")
                            )
                    val publicKeyString =
                            cursor.getString(
                                cursor.getColumnIndex("public_key")
                            )
                    val snText =
                            getString(
                                R.string.sn_text,
                                sn
                            )
                    // 使用数据库中的公鑰生成二維碼
                    Log.e("弓腰",publicKeyString)
                    val qrcodeBitmap =
                            QRCodeUtil().generateQRCode(
                                publicKeyString,
                                200,
                                200
                            )
                    val snCodeBitmap =
                            QRCodeUtil().generateBarcode(
                                sn,
                                200,
                                200
                            )
                    launch(Dispatchers.Main) {
                        textViewSn.text = snText
                        qrCode.setImageBitmap(
                            qrcodeBitmap
                        )
                        snCode.setImageBitmap(
                            snCodeBitmap
                        )
                        layout.visibility =
                                View.VISIBLE
                    }
                    startRabbitMq(
                        sn,
                        privateKeyString
                    )
                } else {
                    val deferred = setCode()
                    launch(Dispatchers.Main) {
                        // 隐藏 ProgressBar
                        textViewSn.text =
                                deferred!!.textViewContent
                        textViewSn.visibility =
                                View.VISIBLE
                        qrCode.setImageBitmap(deferred.qrcodeBitmap)
                        snCode.setImageBitmap(deferred.snCodeBitmap)
                        layout.visibility =
                                View.VISIBLE
                        ToastUtil.getInstance()
                            .showToast(
                                context,
                                "請聯繫it綁定設備"
                            )
                        DelayedProgressDialog.getInstance()
                            .cancel()
                    }
                }

            }
        } catch (e: Exception) {
            ToastUtil.getInstance()
                .showToast(context, "請聯繫it綁定設備 ")
        }

    }

    private suspend fun startRabbitMq(
        sn: String,
        privateKeyString: String?
    ) {
        try {
            coroutineScope {
                val deferred =
                        Channel().deviceRegistration(
                            cid = "media",
                            pac = "com.heyday.media_player",
                            token = sn,
                            context = context
                        )

                val privateKey: PrivateKey =
                        KeyFactory.getInstance(
                            "RSA"
                        ).generatePrivate(
                            PKCS8EncodedKeySpec(
                                Base64.decode(
                                    privateKeyString,
                                    Base64.DEFAULT
                                )
                            )
                        )
                if(deferred != null) {
                    val signData =
                            RSAUtil().signData(
                                deferred,
                                privateKey
                            ).replace("\n", "")
                    val res =
                            Channel().deviceBinding(
                                pac = "com.heyday.media_player",
                                cid = "media",
                                token = sn,
                                uuid = deferred,
                                code = signData,
                                context = context
                            )
                    launch(Dispatchers.Main) {
                        if (res.isNullOrEmpty()) {
                            ToastUtil.getInstance()
                                .showToast(
                                    context,
                                    "請聯繫it綁定設備"
                                )
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                ServiceGlobal.initToken(
                                    uid = "",
                                    token = res["token"]!!
                                )
                                val device = Channel().getDevice(context)
                                ServiceGlobal.brand = device?.meta?.brand ?: ""
                                val deferred = Login().authLogin(
                                    token = res["token"],
                                    provider = "media",
                                    context = context
                                )
                                if (deferred != null) {
                                    ServiceGlobal.initToken(
                                        uid = deferred.data!!.uid,
                                        token = deferred.data!!.token
                                    )
                                    val jwt =
                                            res["token"]?.let {
                                                JWTUtil().jwt(
                                                    it
                                                )
                                            }
                                    if (!res["token"].isNullOrEmpty()) {
                                        RabbitMqManager.Global.sn = res["sn"]!!
                                        RabbitMqManager.Global.token = res["token"]!!
                                        RabbitMqManager.Global.url = (jwt?.get("url") as String?)!!
                                        RabbitMqManager.Global.routingKey = (jwt?.get("routingKey") as String?)!!
                                        RabbitMqManager.Global.queueName = (jwt?.get("queueName") as String?)!!
                                        RabbitMqManager.Global.exchange = (jwt?.get("exchange") as String?)!!
                                        RabbitMqManager.Global.upRoutingKey =
                                                (jwt?.get("upRoutingKey")
                                                    ?: "") as String

                                        startService(
                                            Intent(
                                                context,
                                                RabbitMqService::class.java
                                            )
                                        )

                                        startActivity(
                                            Intent(
                                                context,
                                                MainActivity::class.java
                                            ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            }
                        }
                        DelayedProgressDialog.getInstance()
                            .cancel()
                    }
                } else {
                    launch(Dispatchers.Main) {
                        ToastUtil.getInstance()
                            .showToast(
                                context,
                                "請聯繫it綁定設備"
                            )
                        DelayedProgressDialog.getInstance()
                            .cancel()
                    }
                }
            }
        } catch (e:Exception){
            Log.e("exception",e.toString())
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        cursor.close()
    }

    private fun Context.getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(
            displayMetrics
        )
        return displayMetrics.widthPixels
    }

    private suspend fun setCode(): CodeData? {
        db.delete("authentication_table", null, null)
        val deferred = CompletableDeferred<CodeData?>()
        val sn = RSAUtil().getSN(context) //获取设备SN号
        val keyPair =
                RSAUtil().generateKeyPair() //生成RSA密钥
        val privateKey = keyPair.private
        val publicKey = keyPair.public
        val privateKeyString = Base64.encodeToString(
            privateKey.encoded,
            Base64.DEFAULT
        )
            .replace("\n", "")//将密钥转String
        val publicKeyString = Base64.encodeToString(
            publicKey.encoded,
            Base64.DEFAULT
        )
            .replace("\n", "")
        val snText = getString(R.string.sn_text, sn)
        val qrcodeBitmap =
                QRCodeUtil().generateQRCode(
                    publicKeyString,
                    200,
                    200
                )
        val snCodeBitmap =
                QRCodeUtil().generateBarcode(
                    sn!!,
                    200,
                    200
                )
        val values = ContentValues()
        values.put("sn", sn)
        values.put("private_key", privateKeyString)
        values.put("public_key", publicKeyString)
        db.insert(
            "authentication_table",
            null,
            values
        )
        deferred.complete(
            CodeData(
                snText,
                qrcodeBitmap!!,
                snCodeBitmap!!
            )
        )
        return deferred.await()
    }

    data class CodeData(
        val textViewContent: String,
        val qrcodeBitmap: Bitmap,
        val snCodeBitmap: Bitmap
    )
}