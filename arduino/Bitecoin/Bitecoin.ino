#include <Adafruit_NeoPixel.h>

int neopixelPin = 6;
int ledPin = 13;

// NeoPixel
int numPixels = 16;
Adafruit_NeoPixel stick = Adafruit_NeoPixel(numPixels, neopixelPin, NEO_GRB + NEO_KHZ800);

String prevCommand;
String command;

void setup() {
  // Initialise NeoPixel stick
  stick.begin();
  stick.show(); // initialize all pixels to 'off' 
  
  // Set the LED pin
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);
  
  // Begin the Serial connection
  Serial.begin(9600); // Default connection rate for my BT module
}

void loop() {
  // Read string from Serial if available
  if (Serial.available() > 0) {
    command = Serial.readStringUntil('\n');
  }
  
  // If command isn't empty and not equal to the previous command.
  if (command != "" && command != prevCommand) {
    prevCommand = command;
    
    // Handle it
    handleCommand(command);
  }
}

void handleCommand(String command) {  
  if (command == "led on") {
    digitalWrite(ledPin, HIGH);
    Serial.println("LED on");
  }
  
  if (command == "led off") {
    digitalWrite(ledPin, LOW);
    Serial.println("LED off");
  }
  
  if (command.startsWith("pixel")) {
    int index = command.substring(5, command.length()).toInt();
    setPixelColour(index);
  }
}

void setPixelColour(int index) {
  Serial.print("[setPixelColour] index: ");
  Serial.print(index);
  
  resetPixels();
  
  for (int i = 0; i < index; i++ ) {
    stick.setBrightness(200);
    stick.setPixelColor(i, getColourForIndex(i));
    stick.show();
  }
}

void resetPixels() {
  for (int i = 0; i < numPixels; i++) {
    resetPixel(i);
  }
}

void resetPixel(int index) {
  stick.setPixelColor(index, stick.Color(0, 0, 0));
}

uint32_t getColourForIndex(int index) {
  if (index > numPixels)
    index = numPixels;
    
  switch (index) {
    case 1: return stick.Color(255, 0, 0); // red
    case 2: return stick.Color(255, 50, 0); // 
    case 3: return stick.Color(255, 96, 0); //
    case 4: return stick.Color(255, 128, 0); //
    case 5: return stick.Color(255, 160, 0); // 
    case 6: return stick.Color(255, 192, 0); //
    case 7: return stick.Color(255, 225, 0); // 
    case 8: return stick.Color(255, 255, 0); // yellow
    case 9: return stick.Color(225, 255, 0); // yellow
    case 10: return stick.Color(192, 255, 0); // 
    case 11: return stick.Color(160, 255, 0); // 
    case 12: return stick.Color(128, 255, 0); // 
    case 13: return stick.Color(96, 255, 0); // 
    case 14: return stick.Color(64, 255, 0); //
    case 15: return stick.Color(32, 255, 0); //
    case 16: return stick.Color(0, 255, 0); // green
  }
    
  int red = (255 / numPixels) * index;
  int green = 0;
  int blue = 0;
  
  Serial.println(red);
  
  return stick.Color(red, green, blue);
}
