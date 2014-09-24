mvn clean install -DskipTests
mvn package appassembler:generate-daemons
mvn package appassembler:assemble
chmod +x target/generated-resources/appassembler/jsw/rmd/bin/*
mkdir target/generated-resources/appassembler/jsw/rmd/logs
