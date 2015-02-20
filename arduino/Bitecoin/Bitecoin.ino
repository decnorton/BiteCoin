/* Turn an LED on/off based on a command send via BlueTooth
**
** Credit: The following example was used as a reference
** Rui Santos: http://randomnerdtutorials.wordpress.com
*/

int ledPin = 13;  // use the built in LED on pin 13 of the Uno

String prevCommand;
String command;

void setup() {
    // sets the pins as outputs:
    pinMode(ledPin, OUTPUT);
    digitalWrite(ledPin, LOW);
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
}
