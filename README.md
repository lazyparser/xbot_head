XBot Head
=========

[![Build Status](https://travis-ci.org/lisongting/xbot_head.svg?branch=master)](https://travis-ci.org/lisongting/xbot_head)

## Overview 

XBot Head is an  **android**  application which used for [XBot Robot](http://robots.ros.org/xbot/) .

Xbot Head can recognise specific faces with the support of recognition server.User can register by just a few steps. What is more,it can control the media player of android devices to play audio files that is about  The Software Museum of Chinese Academy of Sciences.

Xbot Head communicates with the Xbot by [Rosbridge_suite Protocal](https://github.com/RobotWebTools/rosbridge_suite/blob/develop/ROSBRIDGE_PROTOCOL.md)   which can ameliorate the interaction  of Ros devices  compared with [RosJava](http://wiki.ros.org/rosjava/).

### About Xbot

* This is the wiki of  Xbot : [http://wiki.ros.org/Robots/Xbot](http://wiki.ros.org/Robots/Xbot) .
* The source code of Xbot:[https://github.com/yowlings/xbot](https://github.com/yowlings/xbot).
* The website:[http://robots.ros.org/xbot](http://robots.ros.org/xbot/).

## [Click here to download Xbot Head Application](http://fir.im/u4rz) 

(This is download link: http://fir.im/u4rz )

## Prerequisite

* Before using this application, please make sure the  Ros Server and the Recognition Server have been started correctly.
* After xbot head application started,the Ip address of Ros Server  and  Ip address of Recognition Server should be configured correctly in setting page of xbot head .

## Features 

1.**User registration** ：User can register into our service by taking  a photo of head portrait.Then the photo will be sent to Recognition Server.At next time the rocognition server will recognise who he/she is. 

2.**Face Recognition & Audio Commentary** ：After face detection and face recognition ,the app will greet to user and then begin to play audio files which is about  The Software Museum of Chinese Academy of Sciences.

3.**Voice Command Word Recognition** ：When android device started to play audio commentary,user can speak out some Chinese words  to control the playing state of media player.

4.**Manipulation** ：This function is still in developing state.



## Ros Topic Statement 

There are two kinds of  **topic**  :

* `/tts_status` :After the commentary audio started , the  backgroud  service of application will  **publish** `/tts_status`continuously. The message used in `/tts_status` is:

  ```
  int32 id
  bool isplaying
  ```

  `int32 id` -- The commentary id that the media player is playing at now.  Initial value is  -1.When first commentary started ,It will changed to 0.

  `bool isplaying` -- Whether the media is playing .

* `/museum_position` ：When application started ,it will **subscribe** this topic in order to know the current status of the movebase.The message used in `/museum_position` is :

  ```
  int32 id
  bool ismoving
  ```

  `int32 id`  -- Current id of area which Xbot is in.

  `bool ismoving` -- Whether the movebase is moving.

## Contributors of this project

- Wei Wu  [lazyparser](https://github.com/lazyparser)
- Songting Li  [lisongting](https://github.com/lisongting)


## Thanks
[bytefish](https://github.com/bytefish/VideoFaceDetection) : a sample Android application for Face Detection  .

[PoiCamera](https://github.com/wuapnjie/PoiCamera) : an Android application by using **android.hardware.camera2** API.

[IFLYTEK](http://www.xfyun.cn/) : an online  service for voice recognition.

[rosbridge_suite](https://github.com/RobotWebTools/rosbridge_suite/blob/develop/ROSBRIDGE_PROTOCOL.md) : RosBridge Protocal.

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
