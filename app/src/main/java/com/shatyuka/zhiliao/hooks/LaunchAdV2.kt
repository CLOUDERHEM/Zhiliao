package com.shatyuka.zhiliao.hooks

import com.shatyuka.zhiliao.Helper

class LaunchAdV2 : BaseHook() {

    private var adUrlKeyList = listOf("/app_float_layer", "/fringe/ad")

    override fun getName(): String {
        return "去启动页广告V2"
    }

    override fun init(classLoader: ClassLoader) {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false)
            || !Helper.prefs.getBoolean("switch_launchad", true)
        ) {
            return
        }
        OkhttpHook.addInterceptors(object : JSONBodyInterceptor {
            override fun match(url: String): Boolean {
                for (adUrlKey in adUrlKeyList) {
                    if (url.contains(adUrlKey)) {
                        return true
                    }
                }
                return false
            }

            override fun intercept(body: String): String {
                return "{}"
            }
        })
    }

    override fun hook() {
        // nop
    }

}
