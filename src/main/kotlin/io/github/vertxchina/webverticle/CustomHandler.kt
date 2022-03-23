package io.github.vertxchina.webverticle

fun handle(handler: suspend () -> Unit, exceptionHandler: suspend (Throwable) -> Unit): suspend () -> Unit {
  return suspend {
    try {
      handler()
    }catch (throwable:Throwable){
      exceptionHandler(throwable)
    }
  }
}