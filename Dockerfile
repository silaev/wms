ARG MONGO_VERSION=4.0.10
FROM mongo:$MONGO_VERSION
COPY scripts/ /scripts/
RUN apt-get update && apt-get install -y \
  nmap \
  dos2unix && dos2unix scripts/** && chmod +x scripts/wait-for-mongo.sh
LABEL version="1.0"
EXPOSE 27017