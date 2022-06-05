import http.client

conn = http.client.HTTPSConnection("jsonplaceholder.typicode.com")

headersList = {
 "Accept": "*/*",
 "User-Agent": "Thunder Client (https://www.thunderclient.com)",
 "Content-Type": "application/json" 
}

payload = json.dumps({
    "userId": 800,
    "name" : "Naslov posta",
    "body" : "Tekst poruke",
    "id": 2
})

conn.request("GET", "/posts?userId=2", payload, headersList)
response = conn.getresponse()
result = response.read()

print(result.decode("utf-8"))