# Make the java runtime image
FROM eclipse-temurin:25-alpine AS jre-build

RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base \
    --add-modules java.logging \
    --add-modules jdk.httpserver \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --output /javaruntime


FROM alpine

COPY /build/docker/monkeymetrics.jar /monkeymetrics.jar

ENV JAVA_HOME=/opt/java/temurin
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

EXPOSE 6969
EXPOSE 9696
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/monkeymetrics.jar"]
