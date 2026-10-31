package com.autolounge.park.car

import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

/**
 * Android Auto bağımsız projeksiyon servisi.
 * Android Auto (Gearhead) araç ekranında doğrudan tam ekran video ve kokpit
 * arayüzünü (AutoLoungeCarActivity) çalıştırmak için bu servisi başlatır.
 */
class AutoLoungeCarService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> {
        return AutoLoungeCarActivity::class.java
    }
}
