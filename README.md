## About ##

This project is aimed to run load tests over the integration API.

In the end it generates a load report in HTML.

### How to run ###

simplest: `./gradlew runLoadtest`

It is possible to pass the following arguments to gradle:

| Parameter | Default value | About |
|-----------|---------------|-------|
| url       | http://localhost:8082/cc-bigdata/integration/ | Which server to call. |
| user      | admin         | The User how runs queries. |
| password  | adminPass     | The password of the user. |
| file      | `100_mols.csv` | Which file to check. Currently `100_mols.csv` is the only available. |
| threads   | 50            | How many users to simulate. |
| chunks    | 5             | How many molecules in one request. |
| output    | report.html   | Where to save output. |
| failOnError | false       | If true java executions ends with an error in case of error. |

#### Examples: ####

```
./gradlew runLoadTest -Purl=http://localhost:8082/cc-bigdata/integration/ -Puser=test -Ppassword=testP
./gradlew runLoadTest -Pthreads=10
./gradlew runLoadTest -Pchunks=60 -PfailOnError=true
```

### Application ###

You can generate a runnable application with the distZip / distTar tasks, and run it. It can have the same settings but in
a different format. You can find information about it in the application's help, which is printed on every run. 