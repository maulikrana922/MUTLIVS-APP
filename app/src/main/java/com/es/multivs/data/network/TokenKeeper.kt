package com.es.multivs.data.network

class TokenKeeper private constructor() {

    var token: String? = null

    companion object {
        var instance: TokenKeeper? = null
            get() {
                if (field == null) field = TokenKeeper()
                return field
            }
            private set
    }
}