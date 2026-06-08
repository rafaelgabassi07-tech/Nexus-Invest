const fs = require('fs');
const path = require('path');
const now = Date.now();
function walk(dir) {
  let results = [];
  const list = fs.readdirSync(dir);
  list.forEach(file => {
    file = path.resolve(dir, file);
    const stat = fs.statSync(file);
    if (stat && stat.isDirectory()) {
      if (!file.includes('node_modules') && !file.includes('.gradle') && !file.includes('.build-outputs') && !file.includes('.git')) {
        results = results.concat(walk(file));
      }
    } else {
      if (now - stat.mtimeMs < 15 * 60 * 1000) {
        results.push(file);
      }
    }
  });
  return results;
}
console.log(walk(process.cwd()).join('\n'));
