package com.example.btlibrary


object BleUUID {
    private val attributes: HashMap<String, String> = HashMap()
    const val MY_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb"
    const val CHARACTERISTIC_A = "0000ffe2-0000-1000-8000-00805f9b34fb"
    const val CHARACTERISTIC_B = "ea49b906-f574-4082-bb68-26a5170cfe91"
    const val DESCRIPTOR_NOTIFICATION = "00002902-0000-1000-8000-00805f9b34fb"


    init {
        // Sample Services.
        attributes[MY_SERVICE] = "My Service"
        // Sample Characteristics.
        attributes[CHARACTERISTIC_A] = "My Characteristic A"
        attributes[CHARACTERISTIC_B] = "My Characteristic B"
    }

    fun lookup(uuid: String?, defaultName: String?): String? {
        val name = attributes[uuid]
        return name ?: defaultName
    }
}