package com.es.multivs.data.bledevices.spyhmomanometer

/**
 * Created by Marko on 11/1/2021.
 * Etrog Systems LTD.
 */
class BloodCuffResults() {
    private var sys = 0
    private var dia = 0

    constructor(sys: Int, dia: Int) : this() {
        this.sys = sys
        this.dia = dia
    }

    fun getSys(): Int {
        return sys
    }


    fun setSys(sys: Int) {
        this.sys = sys
    }

    fun getDia(): Int {
        return dia
    }

    fun setDia(dia: Int) {
        this.dia = dia
    }
}