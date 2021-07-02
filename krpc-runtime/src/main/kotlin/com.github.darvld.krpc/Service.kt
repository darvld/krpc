package com.github.darvld.krpc

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Service(val overrideName: String = "", val providerName: String = "", val clientName: String = "")