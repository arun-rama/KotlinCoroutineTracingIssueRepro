/**
 * The issue is easy to reproduce.
 *
 * Three things need to happen.
 * A. There is a child coroutine scope
 * B. There is a coroutine with its own span
 * C. Coroutine gets suspended and resumes in another thread
 *
 * The bug here is that when the coroutine resumes, then it has incorrect span (it ends up with the parent span)
 *
 *
 * Versions of software (But should be reproduceable on latest versions)
 * Kotlin: 1.7.10
 * "io.opentracing:opentracing-api:0.33.0"
 * "com.datadoghq:dd-trace-ot:1.12.1"
 */


import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import datadog.trace.context.TraceScope
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("scenario")
/**
 * Scenario2
 * Here there are some coroutine scopes created.
 * Specifically, each coroutine has its own coroutine scope
 */
fun scenario() {
  suspend fun simpleFunc(name: String) = coroutineScope {
    withSpan(name) {
      logger.info("$name coroutineContext start")
      delay(1000)
      logger.info("$name coroutineContext done")
    }
  }

  val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
  runBlocking(dispatcher) {
    coroutineScope {
      val asyncCalls = listOf(
        async { simpleFunc("F1") },
        async { simpleFunc("F2") },
        async { simpleFunc("F3") }
      )
      asyncCalls.awaitAll()
    }
  }
}




/**
 * Experimental: convenience function to create a new span.
 */
inline fun <R> withSpan(
  name: String = getDefaultSpanName(),
  tags: Map<String, String> = emptyMap(),
  configureSpan: Tracer.SpanBuilder.() -> Unit = {},
  tracer: Tracer = GlobalTracer.get(),
  block: () -> R
): R {
  val spanBuilder = tracer.buildSpan(name)

  tags.forEach { (key, value) ->
    spanBuilder.withTag(key, value)
  }
  spanBuilder.apply(configureSpan)

  val span = spanBuilder.start()

  val scope = tracer.activateSpan(span)

  // This is useful for coroutines:
  // https://github.com/DataDog/dd-trace-java/issues/931#issuecomment-754597505
  if (scope is TraceScope) {
    scope.setAsyncPropagation(true)
  }

  try {
    return block()
  } finally {
    scope.close()
    span.finish()
  }
}


inline fun getDefaultSpanName(): String {
  val callingStackFrame = Thread.currentThread().stackTrace[1]

  val simpleClassName = Class.forName(callingStackFrame.className).simpleName
  val methodName = callingStackFrame.methodName

  return if (simpleClassName.isNotBlank()) "$simpleClassName.$methodName" else methodName
}
