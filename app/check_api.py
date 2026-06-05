import json
import urllib.request

url = "https://servidor-valorae.vercel.app/api/v1/asset?ticker=WEGE3&view=app"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
resp = urllib.request.urlopen(req)
data = json.loads(resp.read().decode('utf-8'))

print("top level keys:", data.keys())

def find_key(d, tgt, path=""):
    if isinstance(d, dict):
        for k, v in d.items():
            if tgt.lower() in k.lower():
                print(f"FOUND {k} at {path}.{k}")
            find_key(v, tgt, f"{path}.{k}")
    elif isinstance(d, list):
        for i, v in enumerate(d):
            find_key(v, tgt, f"{path}[{i}]")

find_key(data, "revenueBreakdowns")
find_key(data, "negocios")
find_key(data, "geography")
find_key(data, "region")
find_key(data, "receitasLucros")
find_key(data, "revenueProfit")

