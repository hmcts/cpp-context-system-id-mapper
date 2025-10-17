echo "Running Liquibase"
dbServerName=$1
dbUserName=$2
dbPassword=$3
dbPort=${5:-5432}
java -jar system-id-mapper-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/systemidmapper?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi