import fs from 'fs';
import path from 'path';

export default async function handler(req, res) {
  const filePath = path.join(process.cwd(), 'api', 'lib', 'nexus-engine.js');
  
  try {
    const fileContent = fs.readFileSync(filePath, 'utf8');
    
    res.setHeader('Content-Type', 'application/javascript');
    res.setHeader('Content-Disposition', 'attachment; filename=nexus-engine.js');
    res.status(200).send(fileContent);
  } catch (error) {
    res.status(500).json({ error: 'Erro ao ler o arquivo: ' + error.message });
  }
}
