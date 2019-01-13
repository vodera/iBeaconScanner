package com.acuvera.demoapp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.acuvera.demoapp.MainActivity
import com.acuvera.demoapp.R
import com.acuvera.demoapp.model.Device
import com.polidea.rxandroidble2.scan.ScanResult
import kotlinx.android.synthetic.main.device_list.view.*
import java.lang.StringBuilder
import java.util.*
import android.R.attr.data




class DevicesAdapter(private val list: ArrayList<Device>, private val context: Context) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.device_list, parent, false)
        val viewHolder = ViewHolder(view)
        val scanResult = list[position].scanResult
        val device = scanResult.bleDevice
//        viewHolder.textStatus.text = device.connectionState.toString()
        viewHolder.textView.text = device.macAddress
        val data = scanResult.scanRecord.manufacturerSpecificData

        Log.e("SparseArray", " size = ${data.size()}")
        val stringBuilder = StringBuilder()
        for(i in 0 until data.size()) {
            if(data?.valueAt(i) != null) {
                Log.e("ByteArray", "$i = ${data.valueAt(i)}")
                stringBuilder.append(data.valueAt(i).toString())
            }
        }
//        val s = String(data, "UTF-8")
        viewHolder.textMan.text = stringBuilder // scanResult.scanRecord.manufacturerSpecificData.toString()
        // viewHolder.butConnect.setOnClickListener { connectDevice(position) }
        viewHolder.textCountry.text = list[position].country
        viewHolder.txtLat.text = list[position].latitude.toString()
        viewHolder.txtLng.text = list[position].longitude.toString()

        return view
    }

    private fun connectDevice(position: Int) {
        (context as MainActivity).connectDevice(position)
    }

    override fun getItem(position: Int): Any {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return list.size
    }

    class ViewHolder(val view: View) {
        val textView = view.findViewById<TextView>(R.id.text_macaddress)!!
//        val textStatus = view.findViewById<TextView>(R.id.text_status)!!
        //val butConnect = view.findViewById<Button>(R.id.but_connect)!!
        val textMan = view.findViewById<TextView>(R.id.text_manufacturer)
        val textCountry = view.findViewById<TextView>(R.id.text_country)!!
        val txtLat = view.findViewById<TextView>(R.id.text_lat)
        val txtLng = view.findViewById<TextView>(R.id.text_lng)
    }
}

