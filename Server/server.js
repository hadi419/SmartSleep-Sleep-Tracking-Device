// KEY:
/// three forward slashes (///) - signifies a comment that should be removed through workflow process.

const express = require("express");
const bodyParser = require("body-parser");
const fs = require("fs");
const https = require("https");
const bcrypt = require("bcrypt");
const AWS = require("aws-sdk");
const uuid = require('uuid');
const path = require('path');
const mqtt = require('mqtt');
const client = mqtt.connect('mqtt://localhost'); // Mosquitto is running on localhost

const app = express();
const mqttPort = 1883;
let start_stop = false;
let send_signal = false;


client.on('connect', function () {
    console.log('Connected to MQTT broker');
    
    client.subscribe('testTopic', function (err) {
        if (!err) {
            console.log('Subscribed to testTopic');
        }
    });

    /*
    client.subscribe('temperature', function (err) {
        if (!err) {
            console.log('Subscribed to testTopic');
        }
    });*/

    // Subscribe to the general sensor data for any device
    // QoS 1 because it is not sent as regularly so some overhead
    // can be afforded to ensure better package reliability.
    client.subscribe('+/sensor_data', {qos:1}, (err, granted) => {
        if (err) {
            console.error('Subscription error:', err);
        } else {
            console.log('Subscription granted:', granted);
        }
    });

    // client.publish('testTopic', 'Hello from JavaScript!');
});


client.on('message', function (topic, message) {
    console.log('Received message on topic:', topic, 'message:', message.toString());
    try {
        const data = JSON.parse(message);
        console.log('Parsed message:', data);
        const currentTime = new Date();

        const hours = currentTime.getHours();
        const minutes = currentTime.getMinutes();
        const seconds = currentTime.getSeconds();
        const formattedTime = `${hours}:${minutes}:${seconds}`;

        console.log('Received message on topic:', topic, 'message:', message.toString());
        console.log('Current time:', formattedTime);

        const time = formattedTime;
        const heartRate = data.heartRate;
        const remSleep = data.remSleep;
        const temperature = data.temperature;
        const humidity = data.humidity;
        console.log('temperature: ', temperature.toString());
        const update_info = {
            ':heartRate' : heartRate,
            ':remSleep' : remSleep,
            ':temperature' : temperature,
            ':humidity' :   humidity,
        };
        updateAnalytics (time, update_info);
        console.log('Data added to the database:', { time, heartRate, remSleep, temperature, humidity });
      } catch (error) {
        console.error('Error parsing or processing message:', error);
      }

});





app.use(bodyParser.json());

AWS.config.update({ region: 'eu-north-1' }); 

const dynamoDB = new AWS.DynamoDB.DocumentClient();

const userTableName = 'MindFreak';
const sensorDataTableName = 'SensorData';

const privateKeyPath = path.join(__dirname, 'private.key');
const privateKey = fs.readFileSync(privateKeyPath);
const certificatePath = path.join(__dirname, 'certificate.pem');
const certificate = fs.readFileSync(certificatePath);

const sslOptions = {
    key: privateKey,
    cert: certificate,
};

/// This currently isn't used anywhere. 
/// Does it need furhter implementation or removal?
const userSchema = {
    TableName: userTableName,
    KeySchema: [
        { AttributeName: 'email', KeyType: 'HASH' }
    ],
    AttributeDefinitions: [
        { AttributeName: 'email', AttributeType: 'S' },
        { AttributeName: 'password', AttributeType: 'S' },
        { AttributeName: 'gender', AttributeType: 'S' },
        { AttributeName: 'age', AttributeType: 'S' },
        { AttributeName: 'medical', AttributeType: 'S' },
    ],
    ProvisionedThroughput: {
        ReadCapacityUnits: 5,
        WriteCapacityUnits: 5
    }
};

const sensorDataSchema = {
    TableName: sensorDataTableName,
    KeySchema: [
        { AttributeName: 'timestamp', KeyType: 'HASH' }, // timestamp as the primary key
        { AttributeName: 'sensorType', KeyType: 'RANGE' } // sensorType as a sort key
    ],
    AttributeDefinitions: [
        { AttributeName: 'timestamp', AttributeType: 'N' },
        { AttributeName: 'sensorType', AttributeType: 'S' }
    ],
    ProvisionedThroughput: {
        ReadCapacityUnits: 5,
        WriteCapacityUnits: 5
    }
};

app.get("/", (req, res) => {
    console.log("server reached");
    res.send("Hello, this is the root route!");
});

app.get("/test", (req, res) => {
    res.json({ success: true, message: "Server connectivity test successful" });
});

app.post("/analytics", async (req, res) => {
    console.log("Analytics requested");
    try {
        const { email } = req.body;
        const [rem, temperature, temperature_feedback, firstBelow60Time, firstAbove60Time, deepsleep] = await doanalytics();
	console.log([rem, temperature, temperature_feedback, firstBelow60Time, firstAbove60Time, deepsleep]);
        const analytics = {
              sleepscore: "85",
              sleptat: "23:15",
              wokeupat: "7:22",
              asleepfor: "8",
              rem: rem,
              temperature: temperature,
              temperaturefeedback: temperature_feedback,
              deepsleep: deepsleep,
          };

        return res.status(200).json({analytics: analytics});
    } catch (error) {
        console.error('Error getting analytics:', error);
        res.status(500).json({ analytics: null });
    }
});


function calculateTemperatureScore(temperature) {
    const idealMinTemp = 15;
    const idealMaxTemp = 20;
    const scoreRange = 100;
    const midTemp = (idealMaxTemp + idealMinTemp) / 2;

    const distance = Math.abs(temperature - midTemp);
    
    let score;
    if (idealMinTemp<temperature && temperature < idealMaxTemp) {
        score = 100;
    }else{
        score = 100 - 2*distance;
    }

    return score;
}


async function doanalytics() {
    try {
        let rem = "0";
        let temp = "25";
        let temperature_feedback = "";
        let firstBelow60Time = "23:30";
        let firstAbove60Time = "7:45";
        let deepsleep = "20";

        const remPromise = new Promise((resolve, reject) => {
            scanTable('rem', function(err, data) {
                if (err) {
                    reject(err);
                } else {
                    const totalCount = data.Count;
                    const trueCount = data.Items.filter(item => item.rem === true).length;
                    const percentage = (trueCount / totalCount) * 100;
                    console.log('Total REMs:', totalCount);
                    console.log('Total REMs (true):', trueCount);
                    console.log('Percentage of REMs (true):', percentage.toFixed(2) + '%');
                    rem = percentage.toFixed(2);
                    rem = rem.toString();
                    resolve();
                }
            });
        });

        const tempPromise = new Promise((resolve, reject) => {
            scanTable('temperature', function(err, data) {
                if (err) {
                    reject(err);
                } else {
                    const temperatureItems = data.Items.map(item => parseFloat(item.temperature));
                    const sum = temperatureItems.reduce((acc, temperature) => acc + temperature, 0);
                    const averageTemperature = sum / temperatureItems.length;
                    if (averageTemperature < 15) {
                        temperature_feedback = "Too cold environment";
                    } else if (averageTemperature > 20) {
                        temperature_feedback = "Too hot environment";
                    } else {
                        temperature_feedback = "Ideal environment";
                    }
                    temp = averageTemperature.toString()
                    temperaturescore = calculateTemperatureScore(averageTemperature);
                    console.log('Average Temperature:', averageTemperature.toFixed(2) + '°C');
                    console.log('Temperature:', temperaturescore);
                    resolve();
                }
            });
        });

        const heartRatePromise = new Promise((resolve, reject) => {
            scanTable('heartRate', function(err, data) {
                if (err) {
                    reject(err);
                } else {
                    const heartRateItems = data.Items.map(item => parseInt(item.heartRate));

                    let consecutiveBelow60Count = 0;

                    for (let i = 0; i < heartRateItems.length; i++) {
                        const heartRate = heartRateItems[i];
                        if (heartRate < 50) {
                            consecutiveBelow60Count++;
                            if (consecutiveBelow60Count === 1) {
                                firstBelow60Time = data.Items[i].timestamp;
                            }
                        } else {
                            consecutiveBelow60Count = 0;
                        }
                        if (consecutiveBelow60Count === 15) {
                            console.log("start sleep: ", firstBelow60Time)
                            break;
                        }
                    }

                    let consecutiveAbove60Count = 0;

                    for (let i = 0; i < heartRateItems.length; i++) {
                        const heartRate = heartRateItems[i];
                        if (heartRate > 50) {
                            consecutiveAbove60Count++;
                            if (consecutiveAbove60Count === 1) {
                                firstAbove60Time = data.Items[i].timestamp;
                            }
                        } else {
                            consecutiveAbove60Count = 0;
                        }
                        if (consecutiveAbove60Count === 15) {
                            console.log("stop sleep: ", firstAbove60Time)
                        }
                    }


                    const minHeartRate = Math.min(...heartRateItems);

                    const range = minHeartRate * 0.1;

                    let consecutiveInRangeCount = 0;

                    for (let i = 0; i < heartRateItems.length; i++) {
                        const heartRate = heartRateItems[i];

                        if (heartRate >= minHeartRate - range && heartRate <= minHeartRate + range) {
                            consecutiveInRangeCount++;
                        } else {
                            consecutiveInRangeCount = 0;
                        }

                        if (consecutiveInRangeCount === 15) {
                            console.log("Stop sleep - Percentage of times within range:", (consecutiveInRangeCount / heartRateItems.length) * 100 + "%");
                            sleepper = (consecutiveInRangeCount / heartRateItems.length) * 100 + "%"
                            deepsleep = sleepper.toString()
                        }

                    }

                    resolve();
                }
            });
        });

        await Promise.all([remPromise, tempPromise, heartRatePromise]);
        console.log("done analytics");
        return [rem, temp, temperature_feedback, firstBelow60Time, firstAbove60Time, deepsleep];
    } catch (error) {
        console.error("Error in doanalytics:", error);
        return [rem, temp, temperature_feedback, firstBelow60Time, firstAbove60Time, deepsleep];
    }
}

app.post("/signup", async (req, res) => {
    try {
        console.log("signup request");
        const { email, password } = req.body;

        const userExists = await getUserByEmail(email);
        if (userExists) {
            return res.status(200).json({ success: false, message: "Account already exists", user: null });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        const user = {
            email,
            password: hashedPassword,
        };

        await createUser(user);

        res.status(200).json({ success: true, message: "User registered", user: user });

    } catch (error) {
        console.error("Error in signup:", error);
        res.status(400).json({ success: false, message: error.message, user: null });
        console.log("error in signup");
    }
});

app.post("/personal_info", async (req, res) => {
    try {
        console.log("medical request");
        const { email, name, gender, age, medical } = req.body;
        const additional_info = { 
            ':name': name,
            ':age': age,
            ':gender': gender,
            ':medical': medical
        };

        const updatedUser = await updatepersonalinfo(email, additional_info);

        if (updatedUser) {
            console.log('User updated successfully:', updatedUser);
            res.status(200).json({ success: true, message: "User updated successfully", user: updatedUser });
        } else {
            console.error('User not found');
            res.status(200).json({ success: false, message: "User not found", user: null });
        }

    } catch (error) {
        console.error('Error updating user:', error);
        res.status(500).json({ success: false, message: "Error updating user", user: null });
    }
});

app.post("/login", async (req, res) => {
    console.log("login request");
    try {
        const { email, password } = req.body;

        const user = await getUserByEmail(email);

        if (!user) {
            res.status(200).json({ success: false, message: "Account does not exist", user: null });
        } else {
            const isPasswordValid = await bcrypt.compare(password, user.password);

            if (!isPasswordValid) {
                res.status(200).json({ success: false, message: "Wrong password", user: null });
            } else {
                res.status(200).json({ success: true, message: "Login successful", user: user });
            }
        }

    } catch (error) {
        res.status(400).json({ success: false, error: error.message, user: user });
    }
});


app.post("/start_stop", async (req, res) => {
    console.log("start/stop IOT");
    try {
        const { email, signal } = req.body;
        console.log("\nsignal: ", signal);
        console.log(typeof signal);
        var sentsignal = !signal; 
        const user = await getUserByEmail(email);
        const device_id = user.scan;

        /*
        if(sentsignal){
            console.log("start signal received");
            client.subscribe(device_id+'/sensor_data')
        }
        else{
            console.log("stop signal received");
            client.unsubscribe(device_id+'/sensor_data')
        }*/
        
        // console.log("Trying to publish:", device_id+"/control", sentsignal);
        // Sets control signal to be QoS 2 - it is received exactly once
        client.publish(device_id+"/control", sentsignal.toString(), {qos:2}, (err) => {
            if (err) {
                console.error("Error publishing control signal:", err);
              } else {
                console.log("Control Message published with QoS 2");
              }            
        });

        res.status(200).json({ success: true, message: "Starting/stopping now", user: null });
    } catch (error) {
        res.status(400).json({ success: false, error: error.message, user: null });
    }
});


app.post("/device", async (req, res) => {
    try {
        console.log("scan");
        const { email, deviceid} = req.body;
        const additional_info = { 
            ':scan': deviceid
        };
        const updatedUser = await updateDevice(email, additional_info);
        if (updatedUser) {
            console.log('Scan updated successfully:', updatedUser);
            res.status(200).json({ success: true, message: "Scan updated successfully", user: updatedUser });
        } else {
            console.error('User not found');
            res.status(200).json({ success: false, message: "User not found", user: null });
        }
    } catch (error) {
        res.status(400).json({ success: false, error: error.message, user: null });
    }
});


const HTTPS_PORT = 8443;
const HOST = "0.0.0.0";

// Create an HTTPS server
const httpsServer = https.createServer(sslOptions, app);

httpsServer.listen(HTTPS_PORT, HOST, () => {
    console.log(`HTTPS server is listening on https://localhost:${HTTPS_PORT}`);
});

//--- Functions that communicate with DynamoDB ---//
async function getUserByEmail(email) {
    const params = {
        TableName: userTableName,
        Key: {
            email: email
        }
    };

    try {
        const result = await dynamoDB.get(params).promise();
        return result.Item;
    } catch (error) {
        console.error("Error getting user:", error);
        throw error;
    }
}

async function createUser(user) {
    const params = {
        TableName: userTableName,
        Item: user
    };

    try {
        await dynamoDB.put(params).promise();
    } catch (error) {
        console.error("Error creating user:", error);
        throw error;
    }
}

async function updatepersonalinfo(email, additional_info) {
    const params = {
        TableName: userTableName,
        Key: {
            email: email
        },
        UpdateExpression: 'SET #name = :name, #age = :age, #gender = :gender, #medical = :medical',
        ExpressionAttributeNames: {
            '#name': 'name',
            '#age': 'age',
            '#gender': 'gender',
            '#medical': 'medical',
        },
        ExpressionAttributeValues: additional_info,
        ReturnValues: 'ALL_NEW'
    };

    try {
        const result = await dynamoDB.update(params).promise();
        return result.Attributes;
    } catch (error) {
        console.error("Error updating user:", error);
        throw error;
    }
}


async function updateDevice(email, additional_info) {
    const params = {
        TableName: userTableName,
        Key: {
            email: email
        },
        UpdateExpression: 'SET #scan = :scan',
        ExpressionAttributeNames: {
            '#scan': 'scan',
        },
        ExpressionAttributeValues: additional_info,
        ReturnValues: 'ALL_NEW'
    };

    try {
        const result = await dynamoDB.update(params).promise();
        return result.Attributes;
    } catch (error) {
        console.error("Error updating user:", error);
        throw error;
    }
}

async function updateAnalytics(timestamp, update_info) {
    const params = {
        TableName: sensorDataTableName,
        Key: {
            timestamp: timestamp
        },
        
        UpdateExpression: 'SET #heartRate = :heartRate, #remSleep = :remSleep, #temperature = :temperature, #humidity = :humidity',
        ExpressionAttributeNames: {
            '#heartRate' : 'heartRate', 
            '#remSleep' : 'remSleep', 
            '#temperature' : 'temperature', 
            '#humidity' : 'humidity'
        },
        ExpressionAttributeValues: update_info,
        ReturnValues: 'ALL_NEW'
    };

    try {
        const result = await dynamoDB.update(params).promise();
        return result.Attributes; 
    } catch (error) {
        console.error("Error updating user:", error);
        throw error;
    }
}





function scanTable(attributeName, callback) {
    const params = {
        TableName: sensorDataTableName,
        ProjectionExpression: attributeName, 
    };

    dynamoDB.scan(params, function(err, data) {
        if (err) {
            console.error('Unable to scan the table. Error JSON:', JSON.stringify(err, null, 2));
            callback(err, null); 
        } else {
            console.log('Scan succeeded:', data);
            callback(null, data); 
        }
    });
}
