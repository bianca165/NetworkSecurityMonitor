import requests
import random
import time
import multiprocessing
import os  # Adăugat pentru citirea variabilelor de mediu
from pymongo import MongoClient
from datetime import datetime

MONGO_URI = os.environ.get("MONGO_ATLAS_URI")

if not MONGO_URI:
    # Atenție: Acesta este doar un mesaj de eroare, nu un secret
    raise ValueError("Variabila de mediu MONGO_ATLAS_URI nu este setată.")

client = MongoClient(MONGO_URI)
db = client["licenta"]
collection = db["logs"]

USER_AGENTS = [
"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safar>
"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110>
"Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.74 Mobile>
]

def generate_fake_ip():
return f"{random.randint(1, 255)}.{random.randint(1, 255)}.{random.randint(1, 255)}.{random.randint(1, 255)}"


def create_session():
session = requests.Session()
return session

def log_traffic(url, status_code, attack_type="Normal", ip=None):
if ip is None:
ip = generate_fake_ip()

timestamp = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

entry = {
"timestamp": timestamp,
"ip_address": ip,
"url": url,
"status_code": status_code,
"attack_type": attack_type,
"response": "Logged",
"status": "Pending"
}

collection.insert_one(entry)

# trafic normal
def generate_normal_traffic():
session = create_session()
BASE_URL = "http://35.241.129.42:8080"
urls = [f"{BASE_URL}/api/logs", f"{BASE_URL}/api/files", f"{BASE_URL}/api/comments"]
status_codes = [200, 301, 304, 403, 404]

while True:
url = random.choice(urls)
fake_ip = generate_fake_ip()
headers = {"User-Agent": random.choice(USER_AGENTS), "X-Forwarded-For": fake_ip}
try:
response = session.get(url, headers=headers)
status_code = random.choice(status_codes)
print(f"[NORMAL] {url} - IP: {fake_ip} - Status: {status_code}")
log_traffic(url, status_code, "Normal", fake_ip)
except requests.RequestException as e:
print(f"[ERROR] {url}: {e}")
time.sleep(random.uniform(1, 3))


# atac Brute Force
def generate_brute_force():
session = create_session()
BASE_URL = "http://35.241.129.42:8080"
url = f"{BASE_URL}/api/login"
status_codes = [401, 403, 429]

fake_ip = generate_fake_ip()
for _ in range(10):
headers = {"User-Agent": random.choice(USER_AGENTS), "X-Forwarded-For": fake_ip}
data = {"username": "admin", "password": f"password{random.randint(1000, 9999)}"}
try:
response = session.post(url, data=data, headers=headers)
status_code = random.choice(status_codes)
print(f"[BRUTE-FORCE] {url} - IP: {fake_ip} - Status: {status_code}")
log_traffic(url, status_code, "Brute Force", fake_ip)
except requests.RequestException as e:
print(f"[ERROR] brute-force: {e}")
time.sleep(random.uniform(0.1, 0.5))

#sim atac SQL Injection
def generate_sql_injection():
session = create_session()
BASE_URL = "http://35.241.129.42:8080"
url = f"{BASE_URL}/api/search"
queries = ["' OR '1'='1", "'; DROP TABLE users; --", "admin'--"]
status_codes = [500, 403, 200, 404]

fake_ip = generate_fake_ip()
for _ in range(6):
headers = {"User-Agent": random.choice(USER_AGENTS), "X-Forwarded-For": fake_ip}
query = random.choice(queries)
try:
response = session.get(url, params={"q": query}, headers=headers)
status_code = random.choice(status_codes)
print(f"[SQL INJECTION] {url}?q={query} - IP: {fake_ip} - Status: {status_code}")
log_traffic(url, status_code, "SQL Injection", fake_ip)
except requests.RequestException as e:
print(f"[ERROR] SQL Injection: {e}")
time.sleep(random.uniform(0.1, 0.5))

# sim atac path traversal
def generate_path_traversal():
session = create_session()
BASE_URL = "http://35.241.129.42:8080"
url = f"{BASE_URL}/api/files"
paths = ["../../etc/passwd"]
status_codes = [403, 404, 500]

while True:
fake_ip = generate_fake_ip()
headers = {"User-Agent": random.choice(USER_AGENTS), "X-Forwarded-For": fake_ip}
path = random.choice(paths)
try:
response = session.get(f"{url}/{path}", headers=headers)
status_code = random.choice(status_codes)
print(f"[PATH TRAVERSAL] {url}/{path} - IP: {fake_ip} - Status: {status_code}")
log_traffic(url, status_code, "Path Traversal", fake_ip)
except requests.RequestException as e:
print(f"[ERROR] Path Traversal: {e}")
time.sleep(random.uniform(1, 3))

# ddos
def generate_ddos_attack():
session = create_session()
BASE_URL = "http://35.241.129.42:8080"
url = f"{BASE_URL}/api/logs"
status_codes = [200, 503, 429]

while True:
for _ in range(10): # valuri de cereri rapide
fake_ip = generate_fake_ip()
headers = {
"User-Agent": random.choice(USER_AGENTS),
"X-Forwarded-For": fake_ip
}

try:
response = session.get(url, headers=headers)
status_code = random.choice(status_codes)
print(f"[DDoS] {url} - IP: {fake_ip} - Status: {status_code}")
log_traffic(url, status_code, "DDoS", fake_ip)
except requests.RequestException as e:
print(f"[ERROR] DDoS: {e}")
time.sleep(random.uniform(0.1, 0.3)) # interval rapid


if __name__ == "__main__":
processes = [
multiprocessing.Process(target=generate_normal_traffic),
multiprocessing.Process(target=generate_brute_force),
multiprocessing.Process(target=generate_sql_injection),
multiprocessing.Process(target=generate_path_traversal),
multiprocessing.Process(target=generate_ddos_attack)
]


for p in processes:
p.start()


for p in processes:

p.join()
