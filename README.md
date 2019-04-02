[![Maven Central](https://img.shields.io/maven-central/v/fr.uha.ensisa.ff/spark-simple-archetype.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22fr.uha.ensisa.ff%22%20AND%20a%3A%22spark-simple-archetype%22)

# spark-simple-archetype

## Creating a project

Simple maven archetype to startup a Java-based Apache Spark project.

To create your project:

```
mvn archetype:generate \
  -DgroupId=your.group \
  -DartifactId=your_project \
  -DarchetypeGroupId=fr.uha.ensisa.ff \
  -DarchetypeArtifactId=spark-simple-archetype \
  -DarchetypeVersion=0.0.4 \
  -DinteractiveMode=false
```

## Compile project

To compile your project: `mvn clean verify`

To run locally (once compiled): `java -jar target/your_project-SNAPSHOT.jar`

Tu submit to a spark cluster: `spark-submit target/your_project-SNAPSHOT.jar spark://spark-master:7077`

# Running a cluster using docker swarm

You need to run commands using a bash command line (for windows user, use git bash).

## Getting a docker swarm using Vagrant

First install Vagrant and VirtualBox.

The following fires a 3 node cluster (can be changed using the NODES environment variable).

```
git clone --depth=1 https://github.com/fondemen/coreos-swarm-vagrant.git
cd coreos-swarm-vagrant
vagrant box update
vagrant up
```
You might need to select an ethernet interface to connect to ; usually, the first one is best.

To re-use an previously started-then-stopped cluster

```
cd coreos-swarm-vagrant
rm etcd_token_url
vagrant up
```

You must wait for etcd to be up and running on all nodes. Checking can be done using `vagrant ssh docker-01 -c 'etcdctl cluster-health'`

Now, you can build the docker swarm cluster:

```
SWARM=on vagrant up
```

To start running docker commands, ssh the first virtual node:

```
vagrant ssh docker-01
```

Please, check the [coreos-swarm-vagrant](https://github.com/fondemen/coreos-swarm-vagrant) project for more details on setting up the swarm cluster.

## Uploading jar to Vagrant node

The easiest is to install the scp plugin: `vagrant plugin install vagrant-scp`
Compile you project as [explained above](#compile-project)

```
vagrant scp [location of your spark project]/target/target/your_project-SNAPSHOT.jar docker-01:~/ana.jar
```

## Firing up a spark cluster

Not ready for production !

In case you use the Vagrant approach, login to the first node : `vagrant ssh docker-01`.

```
export IMAGE='gettyimages/spark:2.3.1-hadoop-3.0' # check https://hub.docker.com/r/gettyimages/spark/tags
docker image pull $IMAGE
docker tag $IMAGE localhost:5000/spark
docker service create --name registry --publish 5000:5000 registry:2
docker push localhost:5000/spark # so that other nodes get the image
docker network create -d overlay spark-nw
docker service create --name spark-master --replicas 1 --network spark-nw  -e SERVICE_PORTS=7077 -p 7077:7077 -p 8080:8080 -p 6066:6066 localhost:5000/spark bin/spark-class org.apache.spark.deploy.master.Master
docker service create --name spark-worker --mode global --network spark-nw -e SPARK_WORKER_CORES=1 -e SPARK_WORKER_MEMORY=1g --limit-memory 1280M localhost:5000/spark bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077

```

The master (and only the master) is visible at <http://192.168.2.100:8080>.
To see also workers activity, use a proxy such as Sqid :
```
docker service create --name squid --replicas 1 --network spark-nw -p 3128:3128 chrisdaish/squid
```
Then configure your browser to use an HTTP proxy server with `192.168.2.100` as the host and `3128` as the port.

## Submitting your Spark analysis

In case you use the Vagrant approach, you must have [uploaded](#uploading-jar-to-vagrant-node) your Spark program and logged in to docker-01 using `vagrant ssh docker-01`.

We assume here that your Spark analysis is available on ./ana.jar.

### Directly creatting the swarm service

```
docker service create --name ana -p 4040:4040 --network spark-nw --restart-condition none --constraint "node.role == manager" --mount source=$(pwd)/ana.jar,target=/ana.jar,type=bind localhost:5000/spark bin/spark-submit /ana.jar spark://spark-master:7077
```
### Using a dedicated image

Create a file name Dockerfile with the following content using nano:

```
FROM openjdk:8-jre-alpine
COPY ./ana.jar /usr/ana.jar
CMD ["java", "-jar", "/usr/ana.jar", "spark://spark-master:7077"]
```

Build and submit your job to spark:

```
docker build -t ana .
docker tag ana localhost:5000/ana
docker push localhost:5000/ana
docker service create --name ana --restart-condition none --network spark-nw -p 4040:4040 localhost:5000/ana
docker service logs -f ana
```

You can check your task at 

Use ^D to exit logs.

## Stopping the Vagrant swarm cluster

Exit the docker-01 node.

```
vagrant destroy -f
rm etcd_token_url
```
