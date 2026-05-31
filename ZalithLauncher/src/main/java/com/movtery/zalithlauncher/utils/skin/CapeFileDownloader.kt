package com.movtery.zalithlauncher.utils.skin

import com.google.gson.JsonObject
import com.movtery.zalithlauncher.utils.path.UrlManager
import com.movtery.zalithlauncher.utils.stringutils.StringUtils
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.DownloadUtils
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class CapeFileDownloader {
    private val mClient = UrlManager.createOkHttpClient()

    @Throws(Exception::class)
    fun yggdrasil(url: String, capeFile: File, uuid: String) {
        val profileJson = DownloadUtils.downloadString("${url.removeSuffix("/")}/session/minecraft/profile/$uuid")
        val profileObject = Tools.GLOBAL_GSON.fromJson(profileJson, JsonObject::class.java)
        val properties = profileObject.get("properties").asJsonArray
        val rawValue = properties.get(0).asJsonObject.get("value").asString
        val value = StringUtils.decodeBase64(rawValue)
        val valueObject = Tools.GLOBAL_GSON.fromJson(value, JsonObject::class.java)
        val capeUrl = valueObject.get("textures").asJsonObject.get("CAPE").asJsonObject.get("url").asString
        downloadCape(capeUrl, capeFile)
    }

    private fun downloadCape(url: String, capeFile: File) {
        capeFile.parentFile?.apply {
            if (!exists()) mkdirs()
        }

        val request = Request.Builder()
            .url(url)
            .build()

        mClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Unexpected code $response")
            }

            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(capeFile).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }
}
