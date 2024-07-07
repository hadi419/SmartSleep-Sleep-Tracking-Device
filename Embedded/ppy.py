import time
import smbus2
import json
import requests
import matplotlib
import numpy as np
from scipy.signal import firwin, freqz,find_peaks
from scipy.fft import fft
import RPi.GPIO as GPIO
import threading
from scipy.signal import welch

# from mqttfile import client, mqtt_topic_sensor

matplotlib.use('Agg')  # Set the backend to TkAgg

import matplotlib.pyplot as plt
data_lock = threading.Lock()
unprocessed_data_available = threading.Condition()
processed_data_available = threading.Condition()


db = "https://embedded-systems-cw1-default-rtdb.europe-west1.firebasedatabase.app/"

class ppg:

    def __init__(self, sampling_rate, pulse_amplitude, ppg_mode, db):
        self.rrlist = []
        self.flag = False
        GPIO.setmode(GPIO.BCM)
        self.ppg_interrupt_pin = 26
        self.ppg_almost_full_interrupt_pin = 20
        self.heart_rate = None
        self.rem = None
        self.fs = 400.0     # Sampling frequency in Hz
        self.lowcut = 1.0     # Low cut frequency in Hz
        self.highcut = 5.0    # High cut frequency in Hz
        self.numtaps = 100    # Number of taps (Order + 1)
        
        self.bus = smbus2.SMBus(1)
        self.set_sampling_rate(sampling_rate)
        self.set_pulse_amplitude(pulse_amplitude)
        self.set_mode(ppg_mode)
        self.enable_ppg_almost_full_interrupt()
        self.fir_coeff = self.generate_FIR_coeff(self.numtaps, self.lowcut, self.highcut, self.fs)
        self.ppg_almost_full_interrupt_config()
        
        self.data_dump=[]
        self.unprocessed_data_index=0
        self.processed_data_index=0
        self.filtered_data_index=0
  
        self.raw_red_led_vals=[]
        self.raw_ir_vals=[]
        self.filtered_red_led_vals=[]
        self.filtered_ir_vals=[]
        self.db=db
        
        self.data_acquisition = threading.Thread(target=self.data_acquisition)
        self.data_processing= threading.Thread(target=self.data_processing)
        self.data_filtering= threading.Thread(target=self.data_filtering)
   
    MAX30102_ADDR = 0x57
    INTERRUPT_STATUS_1 = 0x00
    INTERRUPT_ENABLE_1 = 0x02
    FIFO_WR_PTR = 0x04
    FIFO_RD_PTR = 0x06
    FIFO_DATA = 0x07
    FIFO_CFG = 0x08
    MODE = 0x09
    SPO2_CFG = 0x0A
    LED1_PA = 0x0C
    LED2_PA = 0x0D
    MULTI_LED_MODE_CTRL = 0x11
    

    def set_mode(self,mode,):
    
        mode_reg_data = self.bus.read_byte_data(self.MAX30102_ADDR, self.MODE)
        
        mode_reg_data &= 0xFC
        mode &= 0x3
        
        data_to_write = mode_reg_data | mode
        
        self.bus.write_byte_data(self.MAX30102_ADDR, self.MODE, data_to_write)

    def set_pulse_amplitude(self, amplitude):
        
        amplitude &= 0xFF
        self.bus.write_byte_data(self.MAX30102_ADDR, self.LED1_PA, amplitude)
        self.bus.write_byte_data(self.MAX30102_ADDR, self.LED2_PA, amplitude)
        
    def set_sampling_rate(self, sampling_rate):
        
        spo2_reg_val = self.bus.read_byte_data(self.MAX30102_ADDR, self.SPO2_CFG)
        
        spo2_reg_val &= 0x1C
        value_to_write = sampling_rate & 0b111
        
        spo2_reg_val |= value_to_write << 2
        
        self.bus.write_byte_data(self.MAX30102_ADDR, self.SPO2_CFG, spo2_reg_val)
        
    def enable_ppg_interrupt(self):
        
        reg_val = self.bus.read_byte_data(self.MAX30102_ADDR, self.INTERRUPT_ENABLE_1)
        reg_val |= 0b1 << 6
        self.bus.write_byte_data(self.MAX30102_ADDR, self.INTERRUPT_ENABLE_1, reg_val)
        
    def enable_ppg_almost_full_interrupt(self):
        
        reg_val = self.bus.read_byte_data(self.MAX30102_ADDR, self.INTERRUPT_ENABLE_1)
        reg_val |= 0b1 << 7
        self.bus.write_byte_data(self.MAX30102_ADDR, self.INTERRUPT_ENABLE_1, reg_val)
        
        fifo_cfg = self.bus.read_byte_data(self.MAX30102_ADDR, self.FIFO_CFG)
        fifo_cfg &= ~0xF
        fifo_cfg |= 0x8
        self.bus.write_byte_data(self.MAX30102_ADDR, self.FIFO_CFG, fifo_cfg)
        
        print(self.bus.read_byte_data(self.MAX30102_ADDR, self.INTERRUPT_ENABLE_1))
        print(self.bus.read_byte_data(self.MAX30102_ADDR, self.FIFO_CFG))
 
     
    def ppg_read_data_callback(self, channel):
        
        print("ppg interrupt detected!!")
        self.read_data(self.MAX30102_ADDR, self.FIFO_DATA, self.bus)
        
    def ppg_read_data_blocks_callback(self, channel):
        
        self.read_block(self.MAX30102_ADDR, self.FIFO_DATA, self.bus)

    def ppg_interrupt_config(self):

        GPIO.setup(self.ppg_interrupt_pin, GPIO.IN, pull_up_down=GPIO.PUD_UP)
        GPIO.add_event_detect(self.ppg_interrupt_pin, GPIO.FALLING, callback= self.ppg_read_data_callback)
        
    def ppg_almost_full_interrupt_config(self):

        GPIO.setup(self.ppg_almost_full_interrupt_pin, GPIO.IN, pull_up_down=GPIO.PUD_UP)
        GPIO.add_event_detect(self.ppg_almost_full_interrupt_pin, GPIO.FALLING, callback= self.ppg_read_data_blocks_callback)
        
    
    def read_data(self, sensor, register, bus):
        
        write = smbus2.i2c_msg.write(sensor, [register])
        read = smbus2.i2c_msg.read(sensor, 6)
        
        bus.i2c_rdwr(write)
        bus.i2c_rdwr(read)
        
        data = list(read)
        
        red_led = data[0] << 16 | data[1] << 8 | data[2]
        red_led &= 0x3FFF
        
        ir_led = data[3] << 16 | data[4] << 8 | data[5]
        ir_led &= 0x3FFF
        
        self.raw_red_led_vals.append(red_led)
        self.raw_ir_vals.append(ir_led)
        
        
    def read_block(self, sensor, register, bus):

        i=0
        while True:
            with unprocessed_data_available:
        
                write = smbus2.i2c_msg.write(sensor, [register])
                read = smbus2.i2c_msg.read(sensor, 6*24)
            
                bus.i2c_rdwr(write)
                bus.i2c_rdwr(read)

                with data_lock:
                    self.data_dump += list(read)
                i+=1
                self.unprocessed_data_index +=24
               
                unprocessed_data_available.notify()
            
            self.bus.read_byte_data(self.MAX30102_ADDR, self.INTERRUPT_STATUS_1)
    
    def process_data_block(self):
        
        i=0
        while True:
            
            with unprocessed_data_available:
                
                while(self.unprocessed_data_index< self.processed_data_index+6):  #new data is not available
                    unprocessed_data_available.wait()
            
                packet = self.data_dump[i:i+6]
                
                red_led = packet[0] << 16 | packet[1] << 8 | packet[2]
                red_led &= 0x3FFF
                with data_lock:
                    self.raw_red_led_vals.append(red_led)
                
                ir_led = packet[3] << 16 | packet[4] << 8 | packet[5]
                ir_led &= 0x3FFF
                
                with data_lock:
                    self.raw_ir_vals.append(ir_led)
                    
                    
                with data_lock:
                    self.raw_red_led_vals.append(ir_led)
                    
            
            self.processed_data_index+=1
            with processed_data_available:
                processed_data_available.notify()

 
            i+=6
          
    def classify_sleep_phase(self, lf_hf_ratio):
            
            rem_threshold = 2.0
            non_rem_threshold = 4.0
            if lf_hf_ratio < non_rem_threshold:
                return 'non-REM'
            elif lf_hf_ratio > rem_threshold:
                return 'REM'
            else:
                return 'Unclassified'
            
        
    def convolve(self):
    
        i=0
        fir_filter = self.fir_coeff
        raw_data = self.raw_ir_vals
        N = len(fir_filter)
        while True:
            with processed_data_available:
                while self.processed_data_index  <= self.filtered_data_index+N:
                    processed_data_available.wait()
            
                dot_product = 0.0
                for j in range(N):
                    dot_product += fir_filter[N-j-1]*raw_data[i+j]
                with data_lock:
                    self.filtered_ir_vals.append(dot_product)
                if i%200 == 0 and i>0:
                    print(i)
                if i%4000 == 0 and i>0:
                    self.flag = True 
                else:
                    self.flag = False
                
                ppg_data={"ppg":dot_product}
                self.filtered_data_index+=1
              
                
                n=4000
                path="ppg/{}.json".format(i)
                if(i%n == 0 and i > 0):
                    peak_calculation_list = self.filtered_ir_vals[(-1*n):-1]
                    peaks, _ = find_peaks(peak_calculation_list,distance=200, prominence=200)
                    print("pulse rate: ", len(peaks)*6)
                    self.heart_rate = len(peaks)*6
                    bpm_data={"heartRate": self.heart_rate}

                    self.rem = "non-rem"
                    self.json_message=json.dumps(bpm_data)

                    dict_db={}
                i+=1
    

    def generate_FIR_coeff(self,numtaps, lowcut, highcut, fs):
        
        return firwin(numtaps, [lowcut, highcut], pass_zero=False, fs=fs)
    
    def collect_data(self):
        try:
        
            while True:
                
                pass
        except KeyboardInterrupt:
        
            GPIO.cleanup()

    def plot_data(self, processed_vals):
        plt.plot(processed_vals)
        plt.savefig('/home/pi/updated_ppg_plot.png')
        
    def data_acquisition(self):
        self.collect_data()
    
    def data_processing(self):
        self.process_data_block()
    
    def data_filtering(self):
        self.convolve()
        
    def start(self):
        
        self.data_acquisition.start()
        time.sleep(2)
        self.data_processing.start()
        time.sleep(2)
        self.data_filtering.start()
        
    def end(self):
        
        self.data_acquisition.join()
        self.data_processing.join()
        self.data_filtering.join()
        


"""
sensor = ppg(0b011, 0x1F, 0x3, db)
sensor.start()
try:
    while True:
        pass

except KeyboardInterrupt:
    GPIO.cleanup



sensor.end()
"""


    
    





    







