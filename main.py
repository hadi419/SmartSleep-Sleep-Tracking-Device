from Embedded.ppy import ppg
from Embedded.temperature_and_humidity import temp_and_hum
import paho.mqtt.client as mqtt
# from mqttfile import runmqtt
import threading
import json
db="https://embedded-systems-cw1-default-rtdb.europe-west1.firebasedatabase.app/"

# *******************MQTT CONNECTION START*******************
mqtt_broker = "13.49.3.225"
mqtt_port = 1883
client_id = "user000_238593"
mqtt_topic_sensor = client_id+"/sensor_data"
mqtt_topic_control = client_id+"/control"
signal = False 
def on_connect(client, userdata, flags, rc):
    print("Connected to MQTT broker with result code "+str(rc))
    client.subscribe(mqtt_topic_control, qos=2)


def on_message(client, userdata, message):
    global signal 
    payload = message.payload.decode("utf-8")
    signal = payload.lower() == "true"
    print("Received signal: {}".format(signal))


client = mqtt.Client(client_id=client_id)
client.on_connect = on_connect
client.on_message = on_message
client.connect(mqtt_broker, mqtt_port, 60)
client.loop_start() 


# *******************MQTT CONNECTION END*******************



ppg_sensor = ppg(0b011, 0x1F, 0x3,db)
temp_and_hum_sensor = temp_and_hum()



ppg_sensor.start()
temp_and_hum_sensor.start()

try:
    while True:
        if(signal):
            if (ppg_sensor.heart_rate != None and temp_and_hum_sensor.temp != None and ppg_sensor.flag):
                sensor_data = {"heartRate": ppg_sensor.heart_rate, "remSleep": ppg_sensor.rem, "temperature": temp_and_hum_sensor.temp, "humidity": temp_and_hum_sensor.hum}
                client.publish(mqtt_topic_sensor, json.dumps(sensor_data), qos=1)

except KeyboardInterrupt:
    GPIO.cleanup()

ppg_sensor.end()
temp_and_hum_sensor.end()
