Redis OSS/Enterprise Installation and Configuration Exercise
============================================================

This details the steps taken to setup the two redis instances 

# Part 1: Installation

## GCP Instance 1 

### Overview
OS: Ubuntu 16

Install Redis OSS version 6.x, on GCP Instance 1

### Install Redis OSS

Steps modified from https://redis.io/download

1.  Add redis labs repository and update packages
```
sudo add-apt-repository ppa:redislabs/redis
sudo apt-get update
```
2. Verify Redis OSS version 6 available
```
apt list -a
```
Sample output -
```
Listing... Done
redis-server/bionic 6:6.0.9-3rl1~bionic1 amd64
redis-server/bionic-updates,bionic-security 5:4.0.9-1ubuntu0.2 amd64
redis-server/bionic 5:4.0.9-1 amd64
```

3. Install Redis OSS version 6
```
sudo apt-get install redis-server=6*
```

*Note* If prompoted to automatically restart services after package install, select "Yes" 

4. Get the ip address of the machine its network by executing ```ifconfig```
The ip address will likely be on the en* adapter - e.g. in this case it is 192.168.0.32
```
ens4: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1460
        inet 192.168.0.32  netmask 255.255.255.255  broadcast 0.0.0.0
```
5. Update the redis.conf so that the redis server listens on its network address and has a master password
    * Run: ```sudo vi /etc/redis/redis.conf ```
    * Replace 
      ```bind 127.0.0.1 ::1```
      with
      ```
      #bind 127.0.0.1 ::1
      bind 127.0.0.1 192.168.0.32:
      ```
      (Note that 192.168.0.32 should be the ip address you found in the previous step)
    * Replace ```#requirepass foobared```
    with
    ```
    #requirepass foobared
    requirepass <some_master_password>
    ```
    where  <some_master_password> is a master password that you set for the redis instance.

6. Restart redis server 
```
sudo /etc/init.d/redis-server restart
```
7. Verify that the server is up and running
```
redis-cli -a <some_master_password> ping
```
where  <some_master_password> the master password that you set previoiusly.

Expect to see the following on the output
```
PONG
```



### Install memtier-benchmark tool

On GCP Instance 1. Steps taken from https://github.com/RedisLabs/memtier_benchmark 

1. Install dependencies
```
sudo apt-get install build-essential autoconf automake libpcre3-dev libevent-dev pkg-config zlib1g-dev libssl-dev
```
2. Download source
```
wget https://github.com/RedisLabs/memtier_benchmark/archive/master.tar.gz -O ~/memtier_benchmark.tar.gz
```
3. Extract source
```
cd ~/
tar xvf memtier_benchmark.tar.gz 
```
4. Build and install source
```
cd ~/memtier_benchmark-master
autoreconf -ivf
./configure
make
sudo make install
```
5. Verify installation
```
memtier_benchmark --help
```
Expect to see help details


## GCP Instance 2

### Overview
OS: Ubuntu 16

Download and install Redis Enterprise GA version on GCP Instance 2 using the no DNS option

### Details

Steps modified from https://docs.redislabs.com/latest/rs/getting-started/ 


1. Create the rlec directory
```
mkdir ~/rlec
```
2.  Download the package
```
cd ~/rlec
wget https://s3.amazonaws.com/redis-enterprise-software-downloads/6.0.8/redislabs-6.0.8-28-bionic-amd64.tar
```
3. Extract the package
```
tar -xvf redislabs-6.0.8-28-bionic-amd64.tar 
```
4. Install correct version of libssl1.1: requires libssl1.1 (>= 1.1.1). This was causing installs to fail.
    * Run: ```sudo apt-cache policy libssl1.1```
    * Choose the version 1.1.1x from the list and install it
    ```
    sudo apt install libssl1.1=1.1.1-1ubuntu2.1~18.04.7
    ```
    * Fix the broken installs
    ```
    sudo apt --fix-broken install
    ```
5. Install Redis Enterprise GA
    * Run: ```sudo ./install.sh -y```
    * Note that port 53 and 21000 are in use, but did not impact the install. The install instructions at https://docs.redislabs.com/latest/rs/getting-started/  appear indicate to disable the default DNS, but the instructions say to install Redis Enterprise GA using the "no DNS option". However the documentation does not indicate (as far as I can see). So I left the existing DNS process running. The process on port 21000 is a wetty process. I also left this running.
    ```
    2021-01-03 00:28:27 [x] Port 53 is occupied.
    2021-01-03 00:28:27 [x] Port 53 is occupied.
    2021-01-03 00:28:27 [x] Port 21000 is occupied.
    ```
6. Fix python google_compute_engine issues
    * To resolve the ```ImportError: No module named google_compute_engine``` errors that were stoping the processes from styarting, the following step was done
    * Run: ```sudo mv /etc/boto.cfg /etc/boto.cfg.old```
    * Note that this just temporarily resolves the google_compute_engine issues seen when running the pything scripts.


# Part 2: Load data using memtier_benchmark

On GCP Instance 1

Adapeted from https://redislabs.com/blog/new-in-memtier_benchmark-pseudo-random-data-gaussian-access-pattern-and-range-manipulation/

1. Load the data
```
memtier_benchmark --random-data --data-size-range=4-204 --data-size-pattern=S --key-minimum=200 --key-maximum=400 --authenticate=<some_master_password>
```
where  <some_master_password> the master password that you set previoiusly

2. Verify the loaded data
    * Run ```redis-cli```
    * Type ```info keyspace```
Expect output similar to 
```
# Keyspace
db0:keys=199,expires=0,avg_ttl=0
```
# Part 3: Setup Redis Enterprise GA Instance

1. Navigate to https://<gcp_instance_2_ip>:8443
2. Follow steps 2 and 3 at https://docs.redislabs.com/latest/rs/getting-started/ to setup the cluster and database
3. Edit the configuration of the Database and set the "Default database access" password
4. Edit the configuration of the Database and set the "Replica of" value to 
```
redis://:<some_master_password>@<gcp_instance_1_ip>:6379
```
where  <gcp_instance_1_ip> is the public ip address of the gcp_instance_1 server.
where  <some_master_password> is a master password that you set for the redis instance on the gcp_instance_1 server.

You should expect that this database is now synched to the database on gcp_instance_1_ip.
