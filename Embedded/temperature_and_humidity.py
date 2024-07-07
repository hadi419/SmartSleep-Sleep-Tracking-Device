import time
import smbus2
import json
import requests, random
import threading

class temp_and_hum:
    
    si7021_ADD = 0x40
    si7021_READ_TEMPERATURE = 0xF3
    si7021_READ_HUMIDITY = 0xF5
    
    
    def collect_data(self):
        
        while True:
            self.bus.i2c_rdwr(self.cmd_meas_temp)
            time.sleep(0.1)
            self.bus.i2c_rdwr(self.read_result)
            temperature = int.from_bytes(self.read_result.buf[0]+self.read_result.buf[1],'big')
            
            
            self.bus.i2c_rdwr(self.cmd_meas_humidity)
            time.sleep(0.1)
            self.bus.i2c_rdwr(self.read_result)
            humidity = int.from_bytes(self.read_result.buf[0]+self.read_result.buf[1],'big')



            self.temp = self.convert_raw_to_temp(temperature)
            self.hum = self.convert_raw_to_humidity(humidity)

            sensor_data = {"temperature": self.temp}
            print(sensor_data)
            self.json_message = json.dumps(sensor_data)
            
    
    
    def convert_raw_to_humidity(self,raw_humidity):
        return ((raw_humidity*125)/65536.0)-6
    
    
    def convert_raw_to_temp(self,raw_temp):
        
        return ((raw_temp*175.72)/65536.0)-46.85
    
    def start(self):
        self.data_acquisition.start()
        
    def end(self):
        self.data_acquisition.join()
    
    
    def __init__(self):
        
        print("hello world")
        self.temp = None
        self.hum = None
        self.bus = smbus2.SMBus(1)
        self.db = "https://embedded-systems-cw1-default-rtdb.europe-west1.firebasedatabase.app/"
        self.json_message = None
        self.cmd_meas_temp = smbus2.i2c_msg.write(self.si7021_ADD, [self.si7021_READ_TEMPERATURE])
        self.cmd_meas_humidity = smbus2.i2c_msg.write(self.si7021_ADD, [self.si7021_READ_HUMIDITY])
        self.read_result = smbus2.i2c_msg.read(self.si7021_ADD, 2)
        self.data_acquisition = threading.Thread(target=self.collect_data)
 

