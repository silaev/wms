ARG MONGO_VERSION=4.0.10
FROM mongo:$MONGO_VERSION
RUN apt-get update && apt-get install -y \
  iputils-ping \
  telnet
COPY scripts/ /scripts/
LABEL version="1.0"
EXPOSE 27017