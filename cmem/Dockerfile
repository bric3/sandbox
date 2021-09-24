FROM centos:centos7

RUN yum install -y java-11-openjdk gcc-c++ glibc-headers glibc-devel

ENV JAVA_HOME=/usr/lib/jvm/jre-11-openjdk

# env JAVA_HOME=/usr/lib/jvm/jre-11-openjdk/ ./gradlew clean build

VOLUME /src
WORKDIR /src
CMD ["bash"]