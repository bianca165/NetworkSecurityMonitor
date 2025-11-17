# NetworkSecurityMonitor

Threat Detection and Response Platform for Cloud Infrastructures

## Overview

NetworkSecurityMonitor is an intelligent security system designed to monitor cloud-based network traffic, detect cyberattacks, and automatically respond by blocking malicious sources.

It integrates multiple advanced technologies:

* **Real-time traffic monitoring**
* **Automatic IP blocking**
* **Secure TLS communication**
* **MongoDB data storage**
* **JavaFX visualization dashboard**
* **Spring Boot backend**
* **Flask attack simulation server**

This platform demonstrates  concepts from Networking and Cybersecurity, including Cloud Networking, Network Security, SDN principles, TLS.

```text
+-------------------------------------------------------------+
|                     JavaFX UI                               |
|  - attack logs, traffic charts, notifications               |
|  - CRUD interface for blocked IPs                           |
+----------------------------▲--------------------------------+
                             | REST API (HTTPS)
+----------------------------▼--------------------------------+
|                     Spring Boot Backend                      |
|  - traffic processing                                       |
|  - attack detection                                     |
|  - IP blocking logic                                        |
|  - communication with MongoDB                               |
+----------------------------▲--------------------------------+
                             | database connector
+----------------------------▼--------------------------------+
|                          MongoDB                             |
|  - attack logs                                              |
|  - traffic events                                           |
|  - blocked IP registry                                      |
+-------------------------------------------------------------+

+-----------------------------+
|    Flask Attack Simulator   |
|  - brute force attempts     |
|  - SQL injection tests      |
|  - fake traffic generator   |
+-----------------------------+
```
## Features
### Real-Time Traffic Monitoring

* The backend continuously analyzes incoming events.


### Automatic IP Blocking

When 5 failed login attempts occur, the system automatically blocks the attacker’s IP.

### Full CRUD for Blocked IPs

You can:

* add
* view 
* update block duration
* remove blocked IPs

### Secure TLS Communication

All sensitive communication is protected using TLS.

### MongoDB Storage

Stores:

* logs
* detected attacks
* blocked IP entries

### JavaFX Dashboard

Includes:

* charts and analytics
* live logs
* notifications
* CSV export
* security controls

### Flask Attack Server

Used for simulations (bruteforce, SQLi, traffic scanning).

|                  | Technology           |
|------------------|----------------------|
| Backend          | Spring Boot, Java 17 |
| Frontend         | JavaFX               |
| Database         | MongoDB Atlas        |
| Security         | TLS 1.3              |
| Attack Simulator | Flask (Python)       |
| Tools            | IntelliJ             |
| Deployment       | Google Cloud VM      |
