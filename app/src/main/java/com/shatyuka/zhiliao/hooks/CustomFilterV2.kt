package com.shatyuka.zhiliao.hooks

import com.shatyuka.zhiliao.Helper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers


class CustomFilterV2 : BaseHook() {

    private val adKeys = listOf("商品", "购买", "咨询", "进店")

    private lateinit var componentCard: Class<*>
    private lateinit var innerDeserializer: Class<*>

    override fun getName(): String {
        return "自定义过滤V2"
    }

    override fun init(classLoader: ClassLoader) {
        componentCard = classLoader.loadClass("com.zhihu.android.ui.shared.sdui.model.Card")
        innerDeserializer =
            classLoader.loadClass("com.zhihu.android.api.util.ZHObjectRegistryCenter\$InnerDeserializer")
    }

    override fun hook() {
        if (!Helper.prefs.getBoolean("switch_mainswitch", false)) {
            return
        }

        XposedBridge.hookAllMethods(innerDeserializer, "deserialize", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.result != null && param.result.javaClass == componentCard) {
                    val del = shouldRemove(param.result)
                    if (del) {
                        param.result = null
                    }
                }
            }
        })

    }

    private fun shouldRemove(card: Any): Boolean {
        if (isExcludedCardType(card)) {
            return true
        }

        val elements = XposedHelpers.getObjectField(card, "elements") as List<*>
        for (element in elements) {
            if (element == null) {
                continue
            }
            if (isExcludedElement(element)) {
                return true
            }
        }
        return false
    }

    private fun isExcludedCardType(card: Any): Boolean {
        val extra = XposedHelpers.getObjectField(card, "extra")
        val contentType = XposedHelpers.getObjectField(extra, "contentType") as String

        if (contentType == "article" && Helper.prefs.getBoolean("switch_removearticle", false)) {
            log("过滤了一篇文章")
            return true
        }
        if (contentType == "pin" && Helper.prefs.getBoolean("switch_pin", false)) {
            log("过滤了一个想法")
            return true
        }
        return false
    }

    private fun isExcludedElement(element: Any): Boolean {
        try {
            if (!(XposedHelpers.getObjectField(element, "visible") as Boolean)) {
                return false
            }
            val type = XposedHelpers.getObjectField(element, "type") as String
            if (type == "Video") {
                log("过滤了一个视频")
                return Helper.prefs.getBoolean("switch_video", false)
            } else if (type == "Line") {
                if (isExcludedSubElement(element)) {
                    return true
                }
            } else if (type == "Text") {
                val id = XposedHelpers.getObjectField(element, "id") as String?
                if (id == "Text") {
                    val text = XposedHelpers.getObjectField(element, "text") as String
                    log("标题: $text")
                    return Helper.regex_title != null && Helper.regex_title.matcher(text).find()
                } else if (id == "text_pin_summary") {
                    val text = XposedHelpers.getObjectField(element, "text") as String
                    log("内容: $text")
                    return Helper.regex_content != null && Helper.regex_content.matcher(text).find()
                }
            }
        } catch (e: Exception) {
            logE("cannot process elements err: $e")
        }
        return false
    }

    private fun isExcludedSubElement(element: Any): Boolean {
        val subElements = XposedHelpers.getObjectField(element, "elements") as List<*>?
        if (subElements == null || subElements.isEmpty()) {
            return false
        }
        for (subElement in subElements) {
            if (!(XposedHelpers.getObjectField(subElement, "visible") as Boolean)) {
                continue
            }
            val subType = XposedHelpers.getObjectField(subElement, "type") as String
            if (subType != "Text") {
                continue
            }

            val text = XposedHelpers.getObjectField(subElement, "text") as String
            if (text.contains("评论")
                || text.contains("赞同")
                || text.contains("收藏")
                || text.contains("浏览")
            ) {
                for (adKey in adKeys) {
                    if (text.contains(adKey)) {
                        return true
                    }
                }
                continue
            }
            log("作者: $text")
            if (Helper.regex_author != null && Helper.regex_author.matcher(text).find()) {
                return true
            }
        }
        return false
    }

    private fun log(msg: String) {
        // logE(msg)
    }
}
