# KotlinCoroutineTracingIssueRepro


This is a project setup to reproduce kotlin coroutine tracing bug with datadog

Original snippet:
https://gist.github.com/arun-rama/38dbd3484cb1eb80fd129f09c9fcc202


# Steps
Download the agent to this directory
(you may have to run build first if the path does not exist)
```
cd app/build/libs;
wget -O dd-java-agent.jar https://dtdg.co/latest-java-tracer
```

To repro the issue
```
./gradlew run
```

Observe the logs.
Log output shows that the span is incorrect.
When `simpleFunc("F1")` stats in thread-2, gets suspended and then resumes in thread-1. It should have the same span id in both the threads. And you can see that is not the case.
```
2023-08-23 18:18:09,353 INFO  [jetty-thread-54] c.s.c.c.hello.HelloModule: Hello
 |- trace_id=1795167897751694888 span_id=4232524128297036031
2023-08-23 18:18:09,418 INFO  [pool-16-thread-2] c.s.c.c.hello.HelloModule: F1 coroutineContext start
 |- trace_id=1795167897751694888 span_id=7896079570348770922
2023-08-23 18:18:09,424 INFO  [pool-16-thread-1] c.s.c.c.hello.HelloModule: F2 coroutineContext start
 |- trace_id=1795167897751694888 span_id=3221523925323319365
2023-08-23 18:18:09,427 INFO  [pool-16-thread-2] c.s.c.c.hello.HelloModule: F3 coroutineContext start
 |- trace_id=1795167897751694888 span_id=6495369003172755865
2023-08-23 18:18:10,432 INFO  [pool-16-thread-2] c.s.c.c.hello.HelloModule: F2 coroutineContext done
 |- trace_id=1795167897751694888 span_id=4232524128297036031
2023-08-23 18:18:10,432 INFO  [pool-16-thread-1] c.s.c.c.hello.HelloModule: F1 coroutineContext done
 |- trace_id=1795167897751694888 span_id=4232524128297036031
2023-08-23 18:18:10,434 INFO  [pool-16-thread-1] c.s.c.c.hello.HelloModule: F3 coroutineContext done
 |- trace_id=1795167897751694888 span_id=4232524128297036031
 ```
