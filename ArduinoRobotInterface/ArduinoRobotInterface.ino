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
  byte msg[0]; // one byte
  int value=10; // value to send,we'll increment and decrement this variable
  if (acc.isConnected())
  {
      // is connected
      int len = acc.read(msg, sizeof(msg), 1); // read data into msg variable
      if (len > 0) {
        if (msg[0] == 1) // compare received data
          digitalWrite(LED_PIN,HIGH); // turn on light
        else
          digitalWrite(LED_PIN,LOW); // turn off light
      }
     
      while(value>0)
      {
          // count down
          msg[0] = value;
          acc.write(msg, 1);
          delay(500);
          value-=1;
      }
  }
}
