Plugin app for Tasker and MacroDroid to provide Humans Detection

Licensed under GPL v3

V 1.3.0
* ADDED: new event allowing to perform a general purpose image analysis using Gemini or Claude

V 1.2.0 
* ADDED: Google Gemini support
* IMPROVED: test file selector will remember the last image (also for file picker)

V 1.1.0 
* remove OpenCV support
* added Tensorflow Lite support
* added Claude.AI (cloud) support: much better reliability !

V 1.0.2
* added file picker 

Features:
* Provides a tasker Event that will trigger when an app (configurable) creates a notification with an image: useful to start a person detection macro that will add advanced AI-features to non-AI enabled cameras
* Provides a tasker action capable to open an image and return a score 0-100 in term of how likely the image contains a person
* Provides a tasker action that you can use to ask generic questions to Claude/Gemini regarding an image/text (e.g. "is the garage door closed?" or "is there a dog in the image?")
* simple home screen to test it against local images
* can parse file names in the form of content://media/external/images/something or in form file:///sdcard/somewhere/file.jpg
* can parse PNG and JPG (tensorflow) or JPG (Anthropic Claude and Google Gemini)

Limitations:
* uses old APIs so no Play store version: you can download pre-built APK from github
* permission and battery management is still rudimentary

Ideas for future improvements:
* Support for PNG with Claude/Gemini
* Support for Openrouter.ai
* support for running locally Google Gemma 3n
* Support for ChatGPT-Vision
* [DONE] ~~Support for generic Claude/ChatGPT actions~~

IMPORTANT Caveats:
* will use more battery than you want, until I understand why, the default plugin mechanism is not working as expected (problems with Foreground service)

HOW-TO use it:
* install the APK (you can download it from the GitHub releases) 
* start it: so that it's registered and available to Tasker/Macrodroid
* (optional): go to settings and add you Claude/Gemini API Key (if you want to be able to use these online models), see <How to create API keys for Google and Claude.md>
* within Macrodroid/Tasker
    * go to the task you want to use
    * add action > external app > HumanDetection4Tasker > Human Recognition
    * you get a window where to enter the name of the image, usually you'll want to use a variable instead of an hard-coded value (e.g. %my_image )
    * then you get a window where you say where to save the result, usually another variable (e.g. detection_score)
    * then do whatever you want with the information :-)

HOW-TO test it:
* start the app
* grant the permission it requires
* use the file picker to choose an image
* press the "test recognition" button
* see what's the score

Example usage for the generic question:
* image: the image you want to analyze
* system prompt:  
* user prompt: <empty>


E.g. my use case is simple: I want to reduce to almost zero the false positive alarms of some security cameras 
* listen for alerts from (cheap?) security cameras
* download the alert image locally
* pass the image to this plugin
* if the detection_score>=50 then start the siren/lights
* otherwise just ignore the false positive


E.g. the AI Image Analysis opens the door to more sofisticated workflows:
* Task: check if I remembered to park the car in the backyard or left it parked in the road in front of the house. I use the backyard security cam to check that
* image: the image from your security camera (I use my plugin here to do that: https://github.com/SimoneAvogadro/CloudEdge4Tasker )
* system prompt: Respond with a single word (CAR or NO_CAR) because your response will be fed into an automation workflow
* user prompt: Please analize the image and respond with a single word: CAR or NO_CAR. If there's a car then return CAR, otherwise return NO_CAR