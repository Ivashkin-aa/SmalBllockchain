FROM openjdk:11
RUN mkdir /node
ADD . /node
RUN apt-get update
RUN cd /node && ./gradlew jar