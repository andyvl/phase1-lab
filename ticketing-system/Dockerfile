FROM eclipse-temurin:25-jre
WORKDIR /work
COPY target/quarkus-app/lib/ /work/lib/
COPY target/quarkus-app/*.jar /work/
COPY target/quarkus-app/app/ /work/app/
COPY target/quarkus-app/quarkus/ /work/quarkus/
EXPOSE 8080
# Generational ZGC: sub-millisecond pauses, never blocks the Vert.x event loop.
# In JDK 25 -XX:+UseZGC always implies the generational mode (non-generational was removed).
# -XX:ZUncommitDelay=30  return unused heap pages to the OS after 30 s of inactivity.
ENTRYPOINT ["java", \
  "--enable-preview", \
  "-XX:+UseZGC", \
  "-XX:ZUncommitDelay=30", \
  "-Xlog:gc*:file=/work/gc-logs/gc.log:time,uptime,level:filecount=3,filesize=10m", \
  "-jar", "/work/quarkus-run.jar"]
