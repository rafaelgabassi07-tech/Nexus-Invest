const fs = require('fs');
let content = fs.readFileSync('app/src/main/java/com/example/network/B3NetworkService.kt', 'utf8');
content = content.replace(/normalized\.optAny\(/g, 'normalized?.optAny(');
content = content.replace(/normalized\.optJSONObject\(/g, 'normalized?.optJSONObject(');
fs.writeFileSync('app/src/main/java/com/example/network/B3NetworkService.kt', content);
