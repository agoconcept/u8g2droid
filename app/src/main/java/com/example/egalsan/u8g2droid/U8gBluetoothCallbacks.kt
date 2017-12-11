package com.example.egalsan.u8g2droid

interface U8gBluetoothCallbacks {
    fun connectionSuccess()
    fun connectionError()

    fun readData(data: String?)
    fun writeData(data: String?)
}
