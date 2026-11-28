package com.robloxbuilder

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class PickImageActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode == RESULT_OK && uri != null) {
            val b64 = ImageHolder.encode(this, uri)
            ImageHolder.listener?.invoke(b64 ?: "")
        }
        ImageHolder.listener = null
        finish()
    }
}
