FROM cangol/android-gradle

ARG NODE_VERSION=12.13.0
ARG IONIC_VERSION=5.4.6
ARG ANDROID_SDK_VERSION=24.4.1
ARG ANDROID_HOME=/opt/android-sdk-linux
ARG GRADLE_VERSION=6.0
ARG PASS_PHRASE=emergya
ARG RELEASE=1.0.0



ENV NODE_VERSION ${NODE_VERSION}
ENV IONIC_VERSION ${IONIC_VERSION}
ENV ANDROID_SDK_VERSION ${ANDROID_SDK_VERSION}
ENV ANDROID_HOME ${ANDROID_HOME}
ENV GRADLE_VERSION ${GRADLE_VERSION}
#ENV LOCAL_KEY_FILE ${LOCAL_KEY_FILE}
ENV PASS_PHRASE ${PASS_PHRASE}
ENV RELEASE ${RELEASE}


# Set time zone
ENV TimeZone=Europe/Stockholm
RUN ln -snf /usr/share/zoneinfo/$TimeZone /etc/localtime && echo $TimeZone > /etc/timezone

WORKDIR /home/gradle

# Install Node
RUN apt-get update &&  \
    apt-get install -y wget curl unzip build-essential gcc make && \
    curl --retry 3 -SLO "http://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.gz" && \
    tar -xzf "node-v$NODE_VERSION-linux-x64.tar.gz" -C /usr/local --strip-components=1 && \
    rm "node-v$NODE_VERSION-linux-x64.tar.gz" && \
    npm install -g ionic@"$IONIC_VERSION"

#install apksigner and zipalignsud
RUN apt-get install -y apksigner
RUN apt-get install -y zipalign

# Setup environment
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:/opt/tools:/opt/gradle/gradle-"$GRADLE_VERSION"/bin


COPY tools/release-key.jks /release-key.jks
COPY tools/entrypoint.sh /entrypoint.sh

#USER gradle

ENTRYPOINT ["/bin/bash","/entrypoint.sh"]
