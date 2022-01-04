FROM adoptopenjdk/maven-openjdk11:latest AS build
ADD . /app
WORKDIR /app
USER root
RUN mvn clean package -Dmaven.test.skip=true

#
# Package stage
#
FROM gcr.io/dataflow-templates-base/java11-template-launcher-base:latest

ARG WORKDIR=/template
ARG MAINCLASS=com.mycloud.ApiToBq
RUN mkdir -p ${WORKDIR}
WORKDIR ${WORKDIR}

COPY --from=build /app/target/mydataflow-1.0.0.jar /template/
#RUN mkdir -p ${WORKDIR}/lib
#COPY --from=build /app/target/lib/ ${WORKDIR}/lib/

ENV FLEX_TEMPLATE_JAVA_CLASSPATH=/template/mydataflow-1.0.0.jar
ENV FLEX_TEMPLATE_JAVA_MAIN_CLASS=${MAINCLASS}