#include <Max3421e.h>
#include <Usb.h>
#include <FHB.h>
#define LED_PIN 3
 
AndroidAccessory acc("DenbarRobotics",
"RobotInterface",
"Description",
"1.0",
"http://yoursite.com",
"0000000012345678");
void setup()
{
  Serial.begin(57600);
  pinMode(LED_PIN, OUTPUT);
  acc.powerOn();
}
 
void loop()
{
  byte msg[128]; 
  //int value=10; // value to send,we'll increment and decrement this variable
  if (acc.isConnected())
  {
      // is connected
      int len = acc.read(msg, sizeof(msg), 128); // read data into msg variable
      if (len > 0) {
        if (msg[0] == 'f') // compare received data
          digitalWrite(LED_PIN,HIGH); // turn on light
        else
          digitalWrite(LED_PIN,LOW); // turn off light
        acc.write(msg, len); // echo the message'
      }     
  }
  delay(10);
}
