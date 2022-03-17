package io.github.vertxchina.persistverticle

import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.multipart.MultipartForm
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch
import java.util.*

class TelegraphImgVerticle : CoroutineVerticle() {

  private val server = "telegra.ph"
  private val uploadUri = "/upload"

  override suspend fun start() {
    super.start()
    vertx.eventBus().consumer<String>(this::class.java.name) { message ->
      launch {
        try {
          val webClient = WebClient.create(vertx)
          var msg = message.body();
          if(msg.startsWith("data:image/") && msg.contains("base64,"))
            msg = msg.substring(msg.indexOf(",")+1)
          val base64 = Base64.getMimeDecoder().decode(msg)

          val file = MultipartForm.create().binaryFileUpload("image", "image", Buffer.buffer().appendBytes(base64), "application/octet-stream")
          val response = webClient.post(443, server, uploadUri).ssl(true).sendMultipartForm(file).await()
          val jsonObject = response.bodyAsJsonArray()
          val url = "https://" + server + jsonObject.getJsonObject(0).getString("src")
          message.reply(url)
        } catch (e:Exception){
          e.printStackTrace()
          message.reply(e.message)
        }
      }
    }
  }

}