# MindFreak


Our system aims to give feedback on sleep quality and provide suggestions on the ideal sleeping environment. Raspberry Pi gets data from the sensor with 4 threads (1 for temperature sensor, 3 for ppg sensor). We implemented interrupts for the ppg sensor to ensure seamless sensor reading and fast data preprocessing on the Raspberry Pi. We process the PPG sensor data on the Raspberry Pi and send the heart rate and REM sleep parameters calculated on the Raspberry PI. It is then processed more at the end of a sleep session to provide information such as percentage of REM sleep, average temperature, a sleep score based on different parameters. 

For getting sleep phases, we used different algorithms. The deep sleep phase happens when the heart rate is at minimum. For REM sleep, we have used the high frequency and low frequency components of the HRV and used a threshold that was stated in the research paper [link](https://www.ahajournals.org/doi/10.1161/01.cir.91.7.1918?url_ver=Z39.88-2003&rfr_id=ori:rid:crossref.org&rfr_dat=cr_pub%20%200pubmed)


We established a MQTT broker on a AWS ec2 instance. This broker sends data from the app server and Raspberry Pi. We developed a HTTPS server with SSL certificates (for more secure communication) on the same ec2 instance for scability that is continously running even if we disconnect from the ec2 instance. We used DyanmoDB services to create a database including a user table and sensor data table. 


## Installation

### Python Dependencies

To install Python dependencies, run the following command:

```bash
pip install -r requirements.txt
```


### Node Dependencies

To install Node dependencies, run the following command:

```bash
npm install
```

## Running Server

The server can be started by running 
```bash
npm start Server/server.js
```

To run it indefinitely, run 

```bash
pm2 start Server/server.js
```

## Running mosquitto broker

Go to Server directory.

The mosquitto broker is started with:

```bash
mosquitto -d -c mosquitto.conf
```

The log from the mosquitto broker goes to Server/output_mosquitto.log,
and for real time viewing of this log while the mosquitto broker is running,
use
```bash
tail -f output_mosquitto.log
```


