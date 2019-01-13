package com.acuvera.demoapp.model

import com.polidea.rxandroidble2.scan.ScanResult

class Device(val scanResult: ScanResult, val country: String, val latitude: Double, val longitude: Double, val city: String)
