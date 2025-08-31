package com.shatyuka.zhiliao.hooks

class CommentAd : BaseHook() {

    override fun getName(): String {
        return "去评论区广告"
    }

    override fun init(classLoader: ClassLoader) {
        OkhttpHook.addInterceptors(object : JSONBodyInterceptor {
            override fun match(url: String): Boolean {
                return url.contains("/root_comment")
            }

            override fun intercept(body: String): String {
                return body.replace("ad_plugin_infos", "ad_plugin_infos_del_del")
                    .replace("atmosphere_voting_config", "atmosphere_voting_config_del")
            }
        })
    }

    override fun hook() {
        // nop
    }
}