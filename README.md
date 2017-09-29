XBot Head
=========

[![Build Status](https://travis-ci.org/lisongting/xbot_head.svg?branch=master)](https://travis-ci.org/lisongting/xbot_head)

## Overview

XBot Head is an  **android**  application which used for [XBot Robot](http://robots.ros.org/xbot/) .There are many ROS package running on Xbot.For more details please visit http://wiki.ros.org/Robots/Xbot/indigo .

Xbot Head can recognise specific faces with the support of recognition server.User can register by just a few steps. What is more,it can control the media player of android devices to play audio files that is about  The Software Museum of Chinese Academy of Sciences.

Xbot Head communicates with the Xbot by [Rosbridge_suite Protocal](https://github.com/RobotWebTools/rosbridge_suite/blob/develop/ROSBRIDGE_PROTOCOL.md)   which can ameliorate the interaction  of Ros devices  compared with [RosJava](http://wiki.ros.org/rosjava/).

### About Xbot

* This is the wiki of  Xbot : [http://wiki.ros.org/Robots/Xbot](http://wiki.ros.org/Robots/Xbot) .
* The source code of Xbot:[https://github.com/yowlings/xbot](https://github.com/yowlings/xbot).
* The website:[http://robots.ros.org/xbot](http://robots.ros.org/xbot/).

## download Xbot Head : http://fir.im/u4rz

## Prerequisite

* Before using this application, please make sure the  Ros Server and the Recognition Server have been started correctly.
* After xbot head application started,the Ip address of Ros Server  and  Ip address of Recognition Server should be configured correctly in setting page of xbot head .

## Features

1.**User registration** ：User can register into our service by taking  a photo of head portrait.Then the photo will be sent to Recognition Server.At next time the rocognition server will recognise who he/she is.

2.**Face Recognition & Audio Commentary ** ：After face detection and face recognition ,the app will greet to user and then begin to play audio files which is about  The Software Museum of Chinese Academy of Sciences.Xbot iscan be used for commentary in many scenes.

3.**Face Sign In Mode ** :Face Sign-in function can be used in common scenes ,such as office and schools.
Xbot Head can  complete this work perfectly with the cooperation of Xbot.

4.**Comprehensive Interaction ** :This function is about AI-Talk mode.It will start conversation between people and Xbot Head.

5.**Manipulation & Controller ** ：There is another application, called [XbotPlayer](https://github.com/lisongting/xbotPlayer) that is used for manipulating the movement of Xbot.



## Ros Topic Statement
### Commentary Mode

There are  two kinds of  **topic** in this mode :

* `/audio_status` :After the commentary audio started , the  backgroud  service of application will  **publish** an `AudioStatus` in topic `/audio_status`. The message used in `/audio_status` is:

  ```
  int32 id
  bool iscomplete
  ```

  `int32 id` -- The commentary id that the media player is playing at now.

  `bool iscomplete` -- The audio file is complete or not.

* `/museum_pos` ：When application started ,it will **subscribe** this topic in order to know the current status of the movebase.When Xbot arrived at a location ,it will publish `MuseumPosition` in this topic .The message used in `/museum_pos` is :

  ```
  int32 id
  bool ismoving
  ```

  `int32 id`  -- Current id of area which Xbot is in.

  `bool ismoving` -- Whether the xbot is moving.


 ### Face Sign Mode
  There are two kinds of topic in this mode.

  * `/robot_status` :When Xbot arrived at a target point ,it will publish a  `RobotStatus` message in `/robot_status` topic .The Message type of `RobotStatus ` is :

```
int32 id
bool ismoving
```
`int32 id`  -- Current id of area which Xbot is in.

`bool ismoving` -- Whether the xbot is moving.

* `pad_sign_completion` :Xbot Head will recognize each person and send the recognition status to Xbot.After each checkpoint finished, it will send a `SignStatus` to Xbot in `pad_sign_completion` topic. The message type of `SignStatus` is :

```
bool complete
bool success
```

`bool complete`  -- Means  whether a checkpoint have been finished by Xbot Head.It shows a recognition server have completed a recognition request or timeout of a recognition request.

 `bool success` -- When this value is `true` ,means a person's face have been recognized successfully .When this value is `false` ,means that recognize failed or timeout of connection to recognition server.

## Contributors of this project

- Wei Wu  [lazyparser](https://github.com/lazyparser)
- Songting Li  [lisongting](https://github.com/lisongting)


## Thanks
[bytefish](https://github.com/bytefish/VideoFaceDetection) : a sample Android application for Face Detection  .

[PoiCamera](https://github.com/wuapnjie/PoiCamera) : an Android application by using **android.hardware.camera2** API.

[IFLYTEK](http://www.xfyun.cn/) : an online  service for voice recognition.

[rosbridge_suite](https://github.com/RobotWebTools/rosbridge_suite/blob/develop/ROSBRIDGE_PROTOCOL.md) : RosBridge Protocal.

[icons8.com](https://icons8.com) : Provide support of Icons.  [The License](https://icons8.com/license/) .

This Project is originally developed by Nguyen Minh Tri - <tri2991@gmail.com>

Original contributors:

* Nguyen Minh Tri  [betri28](https://github.com/betri28)
* xxhong  [hibernate2011](https://github.com/hibernate2011)

---

License
--------

    Copyright 2017 Wei Wu
    Copyright 2017 Songting Li

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
