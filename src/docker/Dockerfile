FROM openjdk:21-slim-bookworm
#create default directories
WORKDIR /
RUN mkdir {template,schema,data,output}
#copy jar
COPY yart.jar /tmp/yart.jar
#default run configuration
CMD java -jar /tmp/yart.jar --templatedirectory /template --schemadirectory /schema --schemafilename schema.json --configfile /data/config.yaml --outputdirectory /output