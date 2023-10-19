FROM openjdk:21-slim-bookworm
#add working user
ARG user=appuser
ARG group=appuser
ARG uid=1000
ARG gid=1000
RUN groupadd -g ${gid} ${group}
RUN useradd -u ${uid} -g ${group} -s /bin/sh -m ${user}
#create default directories
WORKDIR /
RUN mkdir {template,schema,data,output}
#copy jar
COPY yart.jar /tmp/yart.jar
#default run configuration
USER ${uid}:${gid}
CMD java -jar /tmp/yart.jar --templatedirectory /template --schemadirectory /schema --schemafilename schema.json --configfile /data/config.yaml --outputdirectory /output